package com.github.dennisoliveira.portfolio.controller;

import com.github.dennisoliveira.portfolio.domain.Project;
import com.github.dennisoliveira.portfolio.domain.ProjectStatus;
import com.github.dennisoliveira.portfolio.dto.AllocationRequest;
import com.github.dennisoliveira.portfolio.dto.ChangeStatusRequest;
import com.github.dennisoliveira.portfolio.dto.ProjectCreateRequest;
import com.github.dennisoliveira.portfolio.dto.ProjectResponse;
import com.github.dennisoliveira.portfolio.mapper.ProjectMapper;
import com.github.dennisoliveira.portfolio.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Tag(name = "Projects")
public class ProjectController {

    private final ProjectService service;
    private final ProjectMapper mapper;
    private final ProjectService projectService;

    @Operation(summary = "Criar um projeto")
    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectCreateRequest body) {
        Project saved = service.create(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(saved));
    }

    @Operation(summary = "Listar todos os projetos com paginação")
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

    @Operation(summary = "Exibir projeto por id")
    @GetMapping("/{id}")
    public ProjectResponse getById(@PathVariable Long id) {
        Project p = service.getById(id);
        return mapper.toResponse(p);
    }

    @Operation(summary = "Atualizar projeto pelo id")
    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody ProjectCreateRequest body) {
        Project updated = service.update(id, body);
        return mapper.toResponse(updated);
    }

    @Operation(summary = "Excluir projeto pelo id")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @Operation(
        summary = "Atualizar o status do projeto pelo id",
        description = """
          Regras:
          - Não é permitido pular etapas (as transições devem seguir a sequência).
          - EM_ANALISE → ANALISE_REALIZADA → ANALISE_APROVADA → INICIADO → PLANEJADO → EM_ANDAMENTO → ENCERRADO
          - CANCELADO pode ser aplicado a qualquer momento (exceção à ordem).
          - Para ENCERRADO, é obrigatória a actualEndDate (≥ startDate).
        """)
    @PatchMapping("/{id}/status")
    public ProjectResponse changeStatus(@PathVariable Long id, @RequestBody @Valid ChangeStatusRequest body) {
        Project p = service.changeStatus(id, body.newStatus(), body.actualEndDate());
        return mapper.toResponse(p);
    }

    @Operation(
    summary = "Alocar membros em um projeto",
    description = """
      Regras:
      - Apenas membros com role FUNCIONARIO (validados na API externa).
      - Limite do projeto: 1..10 membros.
      - Cada membro: ≤3 projetos ativos (status ≠ ENCERRADO/CANCELADO).
      - Idempotente: reenvios não duplicam.
    """)
    @PostMapping("/{id}/allocations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void allocateMembers(
            @PathVariable Long id,
            @Valid @RequestBody AllocationRequest req
    ) {
        projectService.allocateMembers(id, req.memberExternalIds());
    }

    @Operation(summary = "Remover um membro alocado em um projeto")
    @DeleteMapping("/{id}/allocations/{memberExternalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMemberAllocation(
            @PathVariable Long id,
            @PathVariable String memberExternalId
    ) {
        projectService.removeMemberAllocation(id, memberExternalId);
    }

    @Operation(summary = "Listar os membros alocados em um projeto")
    @GetMapping("/{id}/allocations")
    public List<String> listAllocatedMembers(@PathVariable Long id) {
        return projectService.listAllocatedMembers(id);
    }

}