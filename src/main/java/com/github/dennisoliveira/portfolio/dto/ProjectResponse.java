package com.github.dennisoliveira.portfolio.dto;

import com.github.dennisoliveira.portfolio.domain.ProjectStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectResponse(
        Long id,
        String name,
        LocalDate startDate,
        LocalDate expectedEndDate,
        LocalDate actualEndDate,
        BigDecimal totalBudget,
        String description,
        String managerExternalId,
        ProjectStatus status,
        String risk
) {}