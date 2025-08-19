package com.github.dennisoliveira.portfolio.service;

import com.github.dennisoliveira.portfolio.domain.Project;
import com.github.dennisoliveira.portfolio.domain.ProjectMember;
import com.github.dennisoliveira.portfolio.domain.ProjectMemberId;
import com.github.dennisoliveira.portfolio.domain.ProjectStatus;
import com.github.dennisoliveira.portfolio.dto.ProjectCreateRequest;
import com.github.dennisoliveira.portfolio.exception.BusinessRuleException;
import com.github.dennisoliveira.portfolio.exception.NotFoundException;
import com.github.dennisoliveira.portfolio.integration.members.MemberClient;
import com.github.dennisoliveira.portfolio.mapper.ProjectMapper;
import com.github.dennisoliveira.portfolio.repository.ProjectMemberRepository;
import com.github.dennisoliveira.portfolio.repository.ProjectRepository;
import com.github.dennisoliveira.portfolio.service.domain.RiskClassifier;
import com.github.dennisoliveira.portfolio.service.domain.StatusTransitionValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepo;
    private final ProjectMemberRepository projectMemberRepo;
    private final StatusTransitionValidator transitionValidator;
    private final RiskClassifier riskClassifier;
    private final ProjectMapper mapper;
    private final MemberClient memberClient;

    private static final Set<ProjectStatus> CLOSED_STATUSES =
            EnumSet.of(ProjectStatus.ENCERRADO, ProjectStatus.CANCELADO);

    @Transactional
    public Project create(ProjectCreateRequest dto) {
        Project p = mapper.toEntity(dto);

        validateBudget(p.getTotalBudget());
        validateExpectedVsStart(dto.expectedEndDate(), dto.startDate());

        p.setManagerExternalId(resolveAndValidateManagerId(dto.managerExternalId()));

        if (p.getStatus() == null) {
            p.setStatus(ProjectStatus.EM_ANALISE);
        }

        p.setRisk(riskClassifier.classify(p.getTotalBudget(), p.getStartDate(), p.getExpectedEndDate()));

        return projectRepo.save(p);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<Project> list(
            String name,
            ProjectStatus status,
            String managerExternalId,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            LocalDate expectedEndFrom,
            LocalDate expectedEndTo,
            Pageable pageable
    ) {
        Specification<Project> spec = (root, q, cb) -> cb.conjunction();

        if (name != null && !name.isBlank()) {
            String like = "%" + name.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("name")), like));
        }

        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }

        if (managerExternalId != null && !managerExternalId.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("managerExternalId"), managerExternalId));
        }

        if (startDateFrom != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), startDateFrom));
        }
        if (startDateTo != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("startDate"), startDateTo));
        }

        if (expectedEndFrom != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("expectedEndDate"), expectedEndFrom));
        }
        if (expectedEndTo != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("expectedEndDate"), expectedEndTo));
        }

        return projectRepo.findAll(spec, pageable);
    }

    public Project getById(Long id) {
        return projectRepo.findById(id).orElseThrow(() -> new NotFoundException("Project not found"));
    }

    @Transactional
    public Project update(Long id, ProjectCreateRequest dto) {
        Project p = getById(id);

        p.setName(dto.name());
        p.setStartDate(dto.startDate());
        p.setExpectedEndDate(dto.expectedEndDate());
        p.setActualEndDate(dto.actualEndDate());
        p.setTotalBudget(dto.totalBudget());
        p.setDescription(dto.description());

        validateBudget(p.getTotalBudget());
        validateExpectedVsStart(p.getExpectedEndDate(), p.getStartDate());

        if (!java.util.Objects.equals(p.getManagerExternalId(), dto.managerExternalId())) {
            p.setManagerExternalId(resolveAndValidateManagerId(dto.managerExternalId()));
        }

        p.setRisk(riskClassifier.classify(p.getTotalBudget(), p.getStartDate(), p.getExpectedEndDate()));

        return projectRepo.save(p);
    }

    @Transactional
    public void delete(Long id) {
        Project p = getById(id);
        if (p.getStatus() == ProjectStatus.INICIADO
                || p.getStatus() == ProjectStatus.EM_ANDAMENTO
                || p.getStatus() == ProjectStatus.ENCERRADO) {
            throw new BusinessRuleException("Project cannot be deleted in current status");
        }
        projectRepo.delete(p);
    }

    @Transactional
    public Project changeStatus(Long id, ProjectStatus newStatus, @Nullable LocalDate requestActualEndDate) {
        Project p = getById(id);
        transitionValidator.validate(p.getStatus(), newStatus);

        if (newStatus == ProjectStatus.ENCERRADO) {
            LocalDate end = (requestActualEndDate != null) ? requestActualEndDate : p.getActualEndDate();
            if (end == null) throw new BusinessRuleException("actualEndDate is required when finishing (ENCERRADO)");
            if (end.isBefore(p.getStartDate())) throw new BusinessRuleException("actualEndDate must be >= startDate");
            p.setActualEndDate(end);
        }

        p.setStatus(newStatus);
        return projectRepo.save(p);
    }

    @Transactional
    public List<String> listAllocatedMembers(Long projectId) {
        getById(projectId);
        return projectMemberRepo.findMemberIdsByProject(projectId);
    }

    @Transactional
    public void allocateMembers(Long projectId, List<String> memberExternalIds) {
        if (memberExternalIds == null || memberExternalIds.isEmpty()) {
            throw new BusinessRuleException("You must provide at least one memberExternalId.");
        }

        Set<String> toAllocate = new HashSet<>(memberExternalIds);

        Project project = getById(projectId);

        if (CLOSED_STATUSES.contains(project.getStatus())) {
            throw new BusinessRuleException("Allocations are not allowed for closed/canceled projects.");
        }

        List<String> current = projectMemberRepo.findMemberIdsByProject(projectId);
        Set<String> currentSet = new HashSet<>(current);

        long newOnes = toAllocate.stream().filter(id -> !currentSet.contains(id)).count();
        long finalCount = currentSet.size() + newOnes;
        if (finalCount > 10) {
            throw new BusinessRuleException("Project allocation limit exceeded (max=10).");
        }

        for (String externalId : toAllocate) {

            var maybe = memberClient.getById(externalId);
            if (maybe.isEmpty()) {
                throw new BusinessRuleException(
                        "Member not found in external Members API (id=%s).".formatted(externalId));
            }
            var member = maybe.get();
            if (!member.isFuncionario()) {
                throw new BusinessRuleException(
                        "Only members with role FUNCIONARIO can be allocated (id=%s).".formatted(externalId));
            }

            long activeCount = projectMemberRepo.countActiveProjectsForMember(externalId, CLOSED_STATUSES);

            boolean alreadyHere = currentSet.contains(externalId);
            long effectiveActive = alreadyHere ? activeCount : activeCount + 1;
            if (effectiveActive > 3) {
                throw new BusinessRuleException(
                        "Member exceeds active projects limit (max=3) (id=%s).".formatted(externalId));
            }

            if (!alreadyHere) {
                var id = new ProjectMemberId(projectId, externalId);
                if (!projectMemberRepo.existsById(id)) {
                    try {
                        projectMemberRepo.save(new ProjectMember(project, externalId));
                    } catch (DataIntegrityViolationException ignore) {

                    }
                }
            }
        }

        if (finalCount < 1) {
            throw new BusinessRuleException("Project must have at least 1 allocated member.");
        }
    }

    @Transactional
    public void removeMemberAllocation(Long projectId, String memberExternalId) {
        Objects.requireNonNull(memberExternalId, "memberExternalId is required");

        Project project = getById(projectId);

        if (CLOSED_STATUSES.contains(project.getStatus())) {
            throw new BusinessRuleException("Allocations are not allowed for closed/canceled projects.");
        }

        List<String> current = projectMemberRepo.findMemberIdsByProject(projectId);
        if (!current.contains(memberExternalId)) {
            return;
        }

        if (current.size() <= 1) {
            throw new BusinessRuleException("Project must have at least 1 allocated member.");
        }

        projectMemberRepo.deleteByProjectIdAndMember(projectId, memberExternalId);
    }

    private String resolveAndValidateManagerId(String externalId) {
        var maybe = memberClient.getById(externalId);
        if (maybe.isEmpty()) throw new BusinessRuleException("Manager not found in external Members API id=" + externalId);
        var manager = maybe.get();
        if (!manager.isGerente()) throw new BusinessRuleException("The specified member does not have the MANAGER role");
        return manager.id();
    }

    private void validateBudget(BigDecimal totalBudget) {
        if (totalBudget == null || totalBudget.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("totalBudget must be > 0");
        }
    }

    private void validateExpectedVsStart(LocalDate expectedEnd, LocalDate start) {
        if (expectedEnd.isBefore(start)) {
            throw new BusinessRuleException("expectedEndDate must be >= startDate");
        }
    }
}