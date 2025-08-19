package com.github.dennisoliveira.portfolio.dto;

import com.github.dennisoliveira.portfolio.domain.ProjectStatus;

import java.math.BigDecimal;
import java.util.Map;

public record PortfolioReportResponse(
        Map<ProjectStatus, Long> projectsByStatus,
        Map<ProjectStatus, BigDecimal> totalBudgetByStatus,
        Double avgDurationClosedDays,
        Long uniqueMembersAllocated
) {}
