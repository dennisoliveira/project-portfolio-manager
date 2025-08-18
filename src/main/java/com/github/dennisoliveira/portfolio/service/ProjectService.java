package com.github.dennisoliveira.portfolio.service;

import com.github.dennisoliveira.portfolio.domain.Project;
import com.github.dennisoliveira.portfolio.domain.ProjectStatus;
import com.github.dennisoliveira.portfolio.dto.ProjectCreateRequest;
import com.github.dennisoliveira.portfolio.exception.BusinessRuleException;
import com.github.dennisoliveira.portfolio.exception.NotFoundException;
import com.github.dennisoliveira.portfolio.mapper.ProjectMapper;
import com.github.dennisoliveira.portfolio.repository.MemberRepository;
import com.github.dennisoliveira.portfolio.repository.ProjectRepository;
import com.github.dennisoliveira.portfolio.service.domain.StatusTransitionValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final StatusTransitionValidator transitionValidator;
    private final ProjectRepository projectRepo;
    private final MemberRepository memberRepo;
    private final ProjectMapper mapper;

    @Transactional
    public Project create(ProjectCreateRequest dto) {
        Project p = mapper.toEntity(dto);

        if (p.getTotalBudget() == null || p.getTotalBudget().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("totalBudget must be > 0");
        }
        if (dto.expectedEndDate().isBefore(dto.startDate())) {
            throw new BusinessRuleException("expectedEndDate must be >= startDate");
        }

        var manager = memberRepo.findById(dto.managerId())
                .orElseThrow(() -> new NotFoundException("Manager not found"));
        p.setManager(manager);

        if (p.getStatus() == null) {
            p.setStatus(ProjectStatus.EM_ANALISE);
        }

        return projectRepo.save(p);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<Project> list(
            String name,
            ProjectStatus status,
            Long managerId,
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

        if (managerId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("manager").get("id"), managerId));
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
        p.setTotalBudget(dto.totalBudget());
        p.setDescription(dto.description());

        if (p.getTotalBudget() == null || p.getTotalBudget().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("totalBudget must be > 0");
        }
        if (dto.expectedEndDate().isBefore(dto.startDate())) {
            throw new BusinessRuleException("expectedEndDate must be >= startDate");
        }

        var manager = memberRepo.findById(dto.managerId())
                .orElseThrow(() -> new NotFoundException("Manager not found"));
        p.setManager(manager);

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
            if (end == null) {
                throw new BusinessRuleException("actualEndDate is required when finishing (ENCERRADO)");
            }
            if (end.isBefore(p.getStartDate())) {
                throw new BusinessRuleException("actualEndDate must be >= startDate");
            }
            p.setActualEndDate(end);
        }

        p.setStatus(newStatus);
        return projectRepo.save(p);
    }
}