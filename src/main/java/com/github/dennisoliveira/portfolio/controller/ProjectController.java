package com.github.dennisoliveira.portfolio.controller;

import com.github.dennisoliveira.portfolio.domain.Project;
import com.github.dennisoliveira.portfolio.dto.ProjectCreateRequest;
import com.github.dennisoliveira.portfolio.dto.ProjectResponse;
import com.github.dennisoliveira.portfolio.mapper.ProjectMapper;
import com.github.dennisoliveira.portfolio.service.ProjectService;
import com.github.dennisoliveira.portfolio.service.domain.RiskClassifier;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public List<ProjectResponse> list() {
        return mapper.toResponseList(service.listAll(), riskClassifier);
    }
}