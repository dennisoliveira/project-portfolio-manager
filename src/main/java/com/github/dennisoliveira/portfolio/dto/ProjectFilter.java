package com.github.dennisoliveira.portfolio.dto;

import com.github.dennisoliveira.portfolio.domain.ProjectStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record ProjectFilter(
        String name,
        ProjectStatus status,
        Long managerId,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDateFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDateTo,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate expectedEndFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate expectedEndTo
) {}