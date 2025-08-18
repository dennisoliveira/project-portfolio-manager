package com.github.dennisoliveira.portfolio.service;

import com.github.dennisoliveira.portfolio.domain.Project;
import com.github.dennisoliveira.portfolio.domain.ProjectStatus;
import com.github.dennisoliveira.portfolio.dto.ProjectCreateRequest;
import com.github.dennisoliveira.portfolio.exception.BusinessRuleException;
import com.github.dennisoliveira.portfolio.exception.NotFoundException;
import com.github.dennisoliveira.portfolio.mapper.ProjectMapper;
import com.github.dennisoliveira.portfolio.repository.MemberRepository;
import com.github.dennisoliveira.portfolio.repository.ProjectRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepo;
    private final MemberRepository memberRepo;
    private final ProjectMapper mapper;

    @Transactional
    public Project create(ProjectCreateRequest dto) {
        Project p = mapper.toEntity(dto);

        if (p.getTotalBudget() == null || p.getTotalBudget().compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessRuleException("totalBudget must be > 0");
        if (dto.expectedEndDate().isBefore(dto.startDate()))
            throw new BusinessRuleException("expectedEndDate must be >= startDate");

        var manager = memberRepo.findById(dto.managerId())
                .orElseThrow(() -> new NotFoundException("Manager not found"));
        p.setManager(manager);

        if (p.getStatus() == null) {
            p.setStatus(ProjectStatus.EM_ANALISE);
        }

        return projectRepo.save(p);
    }

    @Transactional
    public List<Project> listAll() {
        return projectRepo.findAll();
    }
}