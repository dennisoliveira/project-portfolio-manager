package com.github.dennisoliveira.portfolio.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectCreateRequest(
        @NotBlank String name,
        @NotNull LocalDate startDate,
        @NotNull LocalDate expectedEndDate,
        LocalDate actualEndDate,
        @NotNull @Positive BigDecimal totalBudget,
        String description,
        @NotNull Long managerId
) {}
