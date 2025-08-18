package com.github.dennisoliveira.portfolio.dto;

import com.github.dennisoliveira.portfolio.domain.ProjectStatus;

import java.time.LocalDate;

public record ChangeStatusRequest(ProjectStatus newStatus, LocalDate actualEndDate) {}