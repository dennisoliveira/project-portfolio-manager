package com.github.dennisoliveira.portfolio.controller;

import com.github.dennisoliveira.portfolio.domain.Project;
import com.github.dennisoliveira.portfolio.domain.ProjectStatus;
import com.github.dennisoliveira.portfolio.dto.ChangeStatusRequest;
import com.github.dennisoliveira.portfolio.dto.ProjectCreateRequest;
import com.github.dennisoliveira.portfolio.dto.ProjectResponse;
import com.github.dennisoliveira.portfolio.mapper.ProjectMapper;
import com.github.dennisoliveira.portfolio.service.ProjectService;
import com.github.dennisoliveira.portfolio.service.domain.RiskClassifier;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Projects")
public class ProjectController {

    private final ProjectService service;
    private final ProjectMapper mapper;
    private final RiskClassifier riskClassifier;

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectCreateRequest body) {
        Project saved = service.create(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(saved));
    }

    @GetMapping
    public Page<ProjectResponse> listPaged(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) String managerExternalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedEndFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedEndTo,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return service.list(name, status, managerExternalId, startDateFrom, startDateTo, expectedEndFrom, expectedEndTo, pageable)
                .map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    public ProjectResponse getById(@PathVariable Long id) {
        Project p = service.getById(id);
        return mapper.toResponse(p);
    }

    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody ProjectCreateRequest body) {
        Project updated = service.update(id, body);
        return mapper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PatchMapping("/{id}/status")
    public ProjectResponse changeStatus(@PathVariable Long id, @RequestBody @Valid ChangeStatusRequest body) {
        Project p = service.changeStatus(id, body.newStatus(), body.actualEndDate());
        return mapper.toResponse(p);
    }

}