package com.github.dennisoliveira.portfolio.controller;

import com.github.dennisoliveira.portfolio.domain.Project;
import com.github.dennisoliveira.portfolio.domain.ProjectStatus;
import com.github.dennisoliveira.portfolio.dto.ProjectCreateRequest;
import com.github.dennisoliveira.portfolio.dto.ProjectResponse;
import com.github.dennisoliveira.portfolio.mapper.ProjectMapper;
import com.github.dennisoliveira.portfolio.service.ProjectService;
import com.github.dennisoliveira.portfolio.service.domain.RiskClassifier;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService service;
    private final ProjectMapper mapper;
    private final RiskClassifier riskClassifier;

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectCreateRequest body) {
        Project saved = service.create(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(saved, riskClassifier));
    }

    @GetMapping
    public Page<ProjectResponse> listPaged(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) Long managerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedEndFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedEndTo,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return service.list(name, status, managerId, startDateFrom, startDateTo, expectedEndFrom, expectedEndTo, pageable)
                .map(p -> mapper.toResponse(p, riskClassifier));
    }
}