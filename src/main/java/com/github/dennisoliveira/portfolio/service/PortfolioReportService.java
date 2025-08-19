package com.github.dennisoliveira.portfolio.service;

import com.github.dennisoliveira.portfolio.domain.ProjectStatus;
import com.github.dennisoliveira.portfolio.dto.PortfolioReportResponse;
import com.github.dennisoliveira.portfolio.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PortfolioReportService {

    private final ProjectRepository projectRepo;

    @Transactional(readOnly = true)
    public PortfolioReportResponse build() {

        Map<ProjectStatus, Long> qtyByStatus = new EnumMap<>(ProjectStatus.class);
        Map<ProjectStatus, BigDecimal> budgetByStatus = new EnumMap<>(ProjectStatus.class);
        for (ProjectStatus s : ProjectStatus.values()) {
            qtyByStatus.put(s, 0L);
            budgetByStatus.put(s, BigDecimal.ZERO);
        }

        for (Object[] row : projectRepo.aggregateByStatusRaw()) {
            ProjectStatus status  = (ProjectStatus) row[0];
            long qty              = ((Number) row[1]).longValue();
            BigDecimal total      = (BigDecimal) row[2];

            qtyByStatus.put(status, qty);
            budgetByStatus.put(status, total != null ? total : BigDecimal.ZERO);
        }

        double avgDays = projectRepo.avgDurationDaysClosedProjects() != null
                ? projectRepo.avgDurationDaysClosedProjects()
                : 0.0;

        long uniqueMembers = projectRepo.countDistinctMembersAllocated() != null
                ? projectRepo.countDistinctMembersAllocated()
                : 0L;

        return new PortfolioReportResponse(qtyByStatus, budgetByStatus, avgDays, uniqueMembers);
    }
}
