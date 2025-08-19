package com.github.dennisoliveira.portfolio.service;

import com.github.dennisoliveira.portfolio.domain.ProjectStatus;
import com.github.dennisoliveira.portfolio.dto.PortfolioReportResponse;
import com.github.dennisoliveira.portfolio.repository.ProjectRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PortfolioReportService {

    private final ProjectRepository projectRepo;

    @Transactional(Transactional.TxType.SUPPORTS)
    public PortfolioReportResponse build() {
        Map<ProjectStatus, Long> qtyByStatus = new EnumMap<>(ProjectStatus.class);
        Map<ProjectStatus, BigDecimal> budgetByStatus = new EnumMap<>(ProjectStatus.class);
        for (ProjectStatus s : ProjectStatus.values()) {
            qtyByStatus.put(s, 0L);
            budgetByStatus.put(s, BigDecimal.ZERO);
        }

        for (Object[] row : projectRepo.countByStatus()) {
            ProjectStatus status = (ProjectStatus) row[0];
            Number qty = (Number) row[1];
            qtyByStatus.put(status, qty.longValue());
        }

        for (Object[] row : projectRepo.sumBudgetByStatus()) {
            ProjectStatus status = (ProjectStatus) row[0];
            BigDecimal total = (BigDecimal) row[1];
            budgetByStatus.put(status, total != null ? total : BigDecimal.ZERO);
        }

        var pairs = projectRepo.findClosedProjectDates();
        double avgDays = 0.0;
        if (!pairs.isEmpty()) {
            long sum = 0L;
            for (Object[] row : pairs) {
                var start = (LocalDate) row[0];
                var end   = (LocalDate) row[1];
                sum += java.time.temporal.ChronoUnit.DAYS.between(start, end);
            }
            avgDays = (double) sum / pairs.size();
        }

        Long uniqueMembers = projectRepo.countDistinctMembersAllocated();
        if (uniqueMembers == null) uniqueMembers = 0L;

        return new PortfolioReportResponse(qtyByStatus, budgetByStatus, avgDays, uniqueMembers);
    }
}
