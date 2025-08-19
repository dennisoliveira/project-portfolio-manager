package com.github.dennisoliveira.portfolio.service;

import com.github.dennisoliveira.portfolio.domain.Project;
import com.github.dennisoliveira.portfolio.domain.ProjectMember;
import com.github.dennisoliveira.portfolio.domain.ProjectMemberId;
import com.github.dennisoliveira.portfolio.domain.ProjectStatus;
import com.github.dennisoliveira.portfolio.dto.ProjectCreateRequest;
import com.github.dennisoliveira.portfolio.exception.BusinessRuleException;
import com.github.dennisoliveira.portfolio.exception.NotFoundException;
import com.github.dennisoliveira.portfolio.integration.members.ExternalMemberDTO;
import com.github.dennisoliveira.portfolio.integration.members.MemberClient;
import com.github.dennisoliveira.portfolio.mapper.ProjectMapper;
import com.github.dennisoliveira.portfolio.repository.ProjectMemberRepository;
import com.github.dennisoliveira.portfolio.repository.ProjectRepository;
import com.github.dennisoliveira.portfolio.service.domain.RiskClassifier;
import com.github.dennisoliveira.portfolio.service.domain.StatusTransitionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock ProjectRepository projectRepo;
    @Mock ProjectMemberRepository projectMemberRepo;
    @Mock StatusTransitionValidator transitionValidator;
    @Mock RiskClassifier riskClassifier;
    @Mock ProjectMapper mapper;
    @Mock MemberClient memberClient;

    @InjectMocks
    ProjectService service;

    private ProjectCreateRequest dto;
    private Project mapped;

    @BeforeEach
    void setUp() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate expected = start.plusMonths(3);

        dto = new ProjectCreateRequest(
                "Projeto X",
                start,
                expected,
                null,
                new BigDecimal("1000"),
                "descricao",
                "mgr-1"
        );

        mapped = new Project();
        mapped.setName("Projeto X");
        mapped.setStartDate(start);
        mapped.setExpectedEndDate(expected);
        mapped.setTotalBudget(new BigDecimal("1000"));
    }

    @Test
    @DisplayName("create: deve definir status padrão EM_ANALISE, validar gerente e salvar projeto")
    void create_shouldSetDefaultStatusAndSave() {
        when(memberClient.getById("mgr-1"))
                .thenReturn(Optional.of(new ExternalMemberDTO("mgr-1", "Alice", "GERENTE")));

        when(mapper.toEntity(dto)).thenReturn(mapped);

        when(riskClassifier.classify(any(), any(), any()))
                .thenReturn(com.github.dennisoliveira.portfolio.domain.Risk.BAIXO);

        when(projectRepo.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        var result = service.create(dto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(ProjectStatus.EM_ANALISE);
        assertThat(result.getManagerExternalId()).isEqualTo("mgr-1");
    }

    @Test
    @DisplayName("create: deve falhar quando o manager informado não for GERENTE")
    void create_shouldFail_whenManagerIsNotGerente() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate expected = start.plusMonths(1);

        var dto = new ProjectCreateRequest(
                "Projeto Y",
                start,
                expected,
                null,
                new BigDecimal("5000"),
                "descricao",
                "func-1"
        );

        var mapped = new Project();
        mapped.setName("Projeto Y");
        mapped.setStartDate(start);
        mapped.setExpectedEndDate(expected);
        mapped.setTotalBudget(new BigDecimal("5000"));

        when(mapper.toEntity(any(ProjectCreateRequest.class))).thenReturn(mapped);

        when(memberClient.getById("func-1"))
                .thenReturn(Optional.of(new ExternalMemberDTO("func-1", "Maria", "FUNCIONARIO")));

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("MANAGER role");

        verify(projectRepo, never()).save(any());
    }

    @Test
    @DisplayName("create: deve falhar quando o manager não é encontrado na Members API")
    void create_shouldFail_whenManagerNotFound() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate expected = start.plusMonths(1);

        var dto = new ProjectCreateRequest(
                "Projeto Z",
                start,
                expected,
                null,
                new BigDecimal("2500"),
                "descricao",
                "mgr-x"
        );

        var mapped = new Project();
        mapped.setName("Projeto Z");
        mapped.setStartDate(start);
        mapped.setExpectedEndDate(expected);
        mapped.setTotalBudget(new BigDecimal("2500"));

        when(mapper.toEntity(any(ProjectCreateRequest.class))).thenReturn(mapped);
        when(memberClient.getById("mgr-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Manager not found");

        verify(projectRepo, never()).save(any());
    }

    @Test
    @DisplayName("create: deve falhar quando o orçamento for <= 0 (não chama Members API nem persiste)")
    void create_shouldFail_whenBudgetNonPositive() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate expected = start.plusMonths(1);

        var dto = new ProjectCreateRequest(
                "Projeto inválido",
                start,
                expected,
                null,
                BigDecimal.ZERO,
                "descricao",
                "mgr-1"
        );

        var mapped = new Project();
        mapped.setName("Projeto inválido");
        mapped.setStartDate(start);
        mapped.setExpectedEndDate(expected);
        mapped.setTotalBudget(BigDecimal.ZERO);

        when(mapper.toEntity(any(ProjectCreateRequest.class))).thenReturn(mapped);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("totalBudget must be > 0");

        verify(memberClient, never()).getById(any());
        verify(projectRepo, never()).save(any());
    }

    @Test
    @DisplayName("create: deve falhar quando data final esperada é anterior à data inicial")
    void create_shouldFail_whenExpectedEndBeforeStart() {
        LocalDate start = LocalDate.of(2025, 5, 1);
        LocalDate expected = LocalDate.of(2025, 4, 30);

        var dto = new ProjectCreateRequest(
                "Projeto com datas inválidas",
                start,
                expected,
                null,
                new BigDecimal("3000"),
                "descricao",
                "mgr-1"
        );

        var mapped = new Project();
        mapped.setName("Projeto com datas inválidas");
        mapped.setStartDate(start);
        mapped.setExpectedEndDate(expected);
        mapped.setTotalBudget(new BigDecimal("3000"));

        when(mapper.toEntity(any(ProjectCreateRequest.class))).thenReturn(mapped);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("expectedEndDate must be >= startDate");

        verify(memberClient, never()).getById(any());
        verify(projectRepo, never()).save(any());
    }

    @Test
    @DisplayName("update: deve reclassificar risco e trocar gerente quando managerExternalId mudar")
    void update_shouldReclassifyRisk_andChangeManager_whenManagerIdChanges() {
        long id = 42L;

        var original = new Project();
        original.setId(id);
        original.setName("Old");
        original.setStartDate(LocalDate.of(2025, 1, 1));
        original.setExpectedEndDate(LocalDate.of(2025, 4, 1));
        original.setTotalBudget(new BigDecimal("1000"));
        original.setManagerExternalId("mgr-1");
        original.setStatus(ProjectStatus.EM_ANALISE);

        when(projectRepo.findById(id)).thenReturn(Optional.of(original));

        var dto = new ProjectCreateRequest(
                "New",
                LocalDate.of(2025, 2, 1),
                LocalDate.of(2025, 5, 1),
                null,
                new BigDecimal("2000"),
                "desc",
                "mgr-2"
        );

        when(memberClient.getById("mgr-2"))
                .thenReturn(Optional.of(new ExternalMemberDTO("mgr-2", "Alice", "GERENTE")));

        when(riskClassifier.classify(new BigDecimal("2000"),
                LocalDate.of(2025, 2, 1),
                LocalDate.of(2025, 5, 1)))
                .thenReturn(com.github.dennisoliveira.portfolio.domain.Risk.MEDIO);

        when(projectRepo.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        var updated = service.update(id, dto);

        assertThat(updated.getName()).isEqualTo("New");
        assertThat(updated.getStartDate()).isEqualTo(LocalDate.of(2025, 2, 1));
        assertThat(updated.getExpectedEndDate()).isEqualTo(LocalDate.of(2025, 5, 1));
        assertThat(updated.getTotalBudget()).isEqualByComparingTo("2000");
        assertThat(updated.getManagerExternalId()).isEqualTo("mgr-2");
        assertThat(updated.getRisk()).isEqualTo(com.github.dennisoliveira.portfolio.domain.Risk.MEDIO);

        verify(memberClient).getById("mgr-2");
    }

    @Test
    @DisplayName("update: não deve chamar Members API quando managerExternalId não mudou (apenas reclassifica risco)")
    void update_shouldNotCallMemberClient_whenManagerIdUnchanged() {
        long id = 43L;

        var original = new Project();
        original.setId(id);
        original.setName("Old");
        original.setStartDate(LocalDate.of(2025, 1, 1));
        original.setExpectedEndDate(LocalDate.of(2025, 4, 1));
        original.setTotalBudget(new BigDecimal("1000"));
        original.setManagerExternalId("mgr-1");
        original.setStatus(ProjectStatus.EM_ANALISE);

        when(projectRepo.findById(id)).thenReturn(Optional.of(original));

        var dto = new ProjectCreateRequest(
                "New",
                LocalDate.of(2025, 2, 1),
                LocalDate.of(2025, 5, 1),
                null,
                new BigDecimal("2000"),
                "desc",
                "mgr-1"
        );

        when(riskClassifier.classify(new BigDecimal("2000"),
                LocalDate.of(2025, 2, 1),
                LocalDate.of(2025, 5, 1)))
                .thenReturn(com.github.dennisoliveira.portfolio.domain.Risk.MEDIO);

        when(projectRepo.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        var updated = service.update(id, dto);

        assertThat(updated.getName()).isEqualTo("New");
        assertThat(updated.getManagerExternalId()).isEqualTo("mgr-1");
        assertThat(updated.getRisk()).isEqualTo(com.github.dennisoliveira.portfolio.domain.Risk.MEDIO);

        verify(memberClient, never()).getById(any());
    }

    @Test
    @DisplayName("delete: deve falhar quando status é INICIADO, EM_ANDAMENTO ou ENCERRADO")
    void delete_shouldFail_whenStatusIsIniciadoOrEmAndamentoOrEncerrado() {
        long id = 99L;

        var p1 = new Project();
        p1.setId(id);
        p1.setStatus(ProjectStatus.INICIADO);

        var p2 = new Project();
        p2.setId(id);
        p2.setStatus(ProjectStatus.EM_ANDAMENTO);

        var p3 = new Project();
        p3.setId(id);
        p3.setStatus(ProjectStatus.ENCERRADO);

        when(projectRepo.findById(id)).thenReturn(Optional.of(p1), Optional.of(p2), Optional.of(p3));

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be deleted");
        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be deleted");
        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be deleted");

        verify(projectRepo, never()).delete(any(Project.class));
    }

    @Test
    @DisplayName("changeStatus: ao ENCERRAR deve exigir actualEndDate válido e sempre delegar ao validador")
    void changeStatus_shouldRequireActualEndDate_whenEncerrado_andDelegateValidator() {
        long id = 77L;

        var p = new Project();
        p.setId(id);
        p.setName("P");
        p.setStartDate(LocalDate.of(2025, 1, 1));
        p.setExpectedEndDate(LocalDate.of(2025, 4, 1));
        p.setStatus(ProjectStatus.EM_ANDAMENTO);

        when(projectRepo.findById(id)).thenReturn(Optional.of(p), Optional.of(p), Optional.of(p));
        when(projectRepo.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.changeStatus(id, ProjectStatus.ENCERRADO, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("actualEndDate is required");

        assertThatThrownBy(() -> service.changeStatus(id, ProjectStatus.ENCERRADO, LocalDate.of(2024, 12, 31)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("actualEndDate must be >= startDate");

        var res = service.changeStatus(id, ProjectStatus.ENCERRADO, LocalDate.of(2025, 2, 1));
        assertThat(res.getStatus()).isEqualTo(ProjectStatus.ENCERRADO);
        assertThat(res.getActualEndDate()).isEqualTo(LocalDate.of(2025, 2, 1));

        verify(transitionValidator, org.mockito.Mockito.times(3))
                .validate(ProjectStatus.EM_ANDAMENTO, ProjectStatus.ENCERRADO);
    }

    @Test
    @DisplayName("allocateMembers: deve falhar quando o projeto estiver ENCERRADO ou CANCELADO")
    void allocate_shouldFail_whenProjectIsClosedOrCanceled() {
        long id = 55L;

        var closed = new Project();
        closed.setId(id);
        closed.setStatus(ProjectStatus.CANCELADO);

        when(projectRepo.findById(id)).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> service.allocateMembers(id, java.util.List.of("m1")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("closed/canceled");

        verify(memberClient, never()).getById(any());
        verify(projectMemberRepo, never()).findMemberIdsByProject(id);
    }

    @Test
    @DisplayName("allocateMembers: deve falhar quando exceder limite de 10 considerando alocados + novos")
    void allocate_shouldFail_whenExceedingMax10ConsideringExisting() {
        long id = 1L;

        var p = new Project();
        p.setId(id);
        p.setStatus(ProjectStatus.EM_ANALISE);

        when(projectRepo.findById(id)).thenReturn(Optional.of(p));
        when(projectMemberRepo.findMemberIdsByProject(id))
                .thenReturn(java.util.List.of("m1","m2","m3","m4","m5","m6","m7","m8","m9"));

        assertThatThrownBy(() -> service.allocateMembers(id, java.util.List.of("m10","m11")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("limit exceeded");

        verify(memberClient, never()).getById(any());
    }

    @Test
    @DisplayName("allocateMembers: deve falhar quando o membro não é encontrado na Members API")
    void allocate_shouldFail_whenMemberNotFound() {
        long projectId = 2L;

        var p = new Project();
        p.setId(projectId);
        p.setStatus(ProjectStatus.EM_ANALISE);

        when(projectRepo.findById(projectId)).thenReturn(Optional.of(p));
        when(projectMemberRepo.findMemberIdsByProject(projectId)).thenReturn(java.util.List.of());
        when(memberClient.getById("m1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.allocateMembers(projectId, java.util.List.of("m1")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Member not found");

        verify(projectMemberRepo, never()).countActiveProjectsForMember(any(), anySet());
        verify(projectMemberRepo, never()).save(any());
    }

    @Test
    @DisplayName("allocateMembers: deve falhar quando o membro não tiver role FUNCIONARIO")
    void allocate_shouldFail_whenMemberIsNotFuncionario() {
        long projectId = 3L;

        var p = new Project();
        p.setId(projectId);
        p.setStatus(ProjectStatus.EM_ANALISE);

        when(projectRepo.findById(projectId)).thenReturn(Optional.of(p));
        when(projectMemberRepo.findMemberIdsByProject(projectId)).thenReturn(java.util.List.of());

        when(memberClient.getById("m2"))
                .thenReturn(Optional.of(new ExternalMemberDTO("m2", "João", "GERENTE")));

        assertThatThrownBy(() -> service.allocateMembers(projectId, java.util.List.of("m2")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only members with role FUNCIONARIO");

        verify(projectMemberRepo, never()).countActiveProjectsForMember(any(), org.mockito.ArgumentMatchers.anySet());
        verify(projectMemberRepo, never()).save(any());
    }

    @Test
    @DisplayName("allocateMembers: deve respeitar limite de 3 projetos ativos por membro, sem contar quando já está alocado")
    void allocate_shouldEnforceMemberMax3Active_consideringAlreadyAllocated() {
        long projectId = 4L;

        var p = new Project();
        p.setId(projectId);
        p.setStatus(ProjectStatus.EM_ANALISE);

        when(projectRepo.findById(projectId)).thenReturn(Optional.of(p));
        when(projectMemberRepo.findMemberIdsByProject(projectId)).thenReturn(java.util.List.of("m1"));

        when(memberClient.getById("m1"))
                .thenReturn(Optional.of(new ExternalMemberDTO("m1", "Ana", "FUNCIONARIO")));
        when(memberClient.getById("m2"))
                .thenReturn(Optional.of(new ExternalMemberDTO("m2", "Bruno", "FUNCIONARIO")));

        when(projectMemberRepo.countActiveProjectsForMember(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anySet())
        ).thenReturn(3L);

        assertThatThrownBy(() -> service.allocateMembers(projectId, java.util.List.of("m1", "m2")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("active projects limit (max=3)");

        verify(projectMemberRepo, never()).save(any());
    }

    @Test
    @DisplayName("allocateMembers: deve salvar apenas os novos e ignorar DataIntegrityViolation (concorrência)")
    void allocate_shouldSaveOnlyNew_andIgnoreDataIntegrityRace() {
        long projectId = 5L;

        var p = new Project();
        p.setId(projectId);
        p.setStatus(ProjectStatus.EM_ANALISE);

        when(projectRepo.findById(projectId)).thenReturn(Optional.of(p));
        when(projectMemberRepo.findMemberIdsByProject(projectId)).thenReturn(java.util.List.of("m1"));

        when(memberClient.getById(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> {
                    String id = inv.getArgument(0);
                    return Optional.of(new ExternalMemberDTO(id, "Nome-" + id, "FUNCIONARIO"));
                });

        when(projectMemberRepo.countActiveProjectsForMember(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anySet()
        )).thenReturn(0L);

        when(projectMemberRepo.existsById(org.mockito.ArgumentMatchers.any(ProjectMemberId.class))).thenReturn(false);

        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0))
                .when(projectMemberRepo)
                .save(org.mockito.ArgumentMatchers.argThat(pm -> ((ProjectMember) pm).getMemberExternalId().equals("m2")));

        org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException("dup"))
                .when(projectMemberRepo)
                .save(org.mockito.ArgumentMatchers.argThat(pm -> ((ProjectMember) pm).getMemberExternalId().equals("m3")));

        service.allocateMembers(projectId, java.util.List.of("m1", "m2", "m3"));

        org.mockito.Mockito.verify(projectMemberRepo, org.mockito.Mockito.never())
                .save(org.mockito.ArgumentMatchers.argThat(pm -> ((ProjectMember) pm).getMemberExternalId().equals("m1")));
        org.mockito.Mockito.verify(projectMemberRepo)
                .save(org.mockito.ArgumentMatchers.argThat(pm -> ((ProjectMember) pm).getMemberExternalId().equals("m2")));
        org.mockito.Mockito.verify(projectMemberRepo)
                .save(org.mockito.ArgumentMatchers.argThat(pm -> ((ProjectMember) pm).getMemberExternalId().equals("m3")));
    }

    @Test
    @DisplayName("removeMemberAllocation: deve falhar quando o projeto estiver ENCERRADO ou CANCELADO")
    void removeAllocation_shouldFail_whenProjectIsClosedOrCanceled() {
        long projectId = 6L;

        var p = new Project();
        p.setId(projectId);
        p.setStatus(ProjectStatus.ENCERRADO);

        when(projectRepo.findById(projectId)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.removeMemberAllocation(projectId, "m1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("closed/canceled");

        verify(projectMemberRepo, never()).findMemberIdsByProject(anyLong());
        verify(projectMemberRepo, never()).deleteByProjectIdAndMember(
                anyLong(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("removeMemberAllocation: deve impedir remover o último membro (mínimo 1 alocado)")
    void removeAllocation_shouldFail_whenRemovingLastMember() {
        long projectId = 7L;

        var p = new Project();
        p.setId(projectId);
        p.setStatus(ProjectStatus.EM_ANALISE);

        when(projectRepo.findById(projectId)).thenReturn(Optional.of(p));
        when(projectMemberRepo.findMemberIdsByProject(projectId))
                .thenReturn(java.util.List.of("m1"));

        assertThatThrownBy(() -> service.removeMemberAllocation(projectId, "m1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at least 1 allocated member");

        verify(projectMemberRepo, never())
                .deleteByProjectIdAndMember(anyLong(),
                        org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("removeMemberAllocation: deve não fazer nada quando o membro não está alocado")
    void removeAllocation_shouldDoNothing_whenMemberNotAllocated() {
        long projectId = 8L;

        var p = new Project();
        p.setId(projectId);
        p.setStatus(ProjectStatus.EM_ANALISE);

        when(projectRepo.findById(projectId)).thenReturn(Optional.of(p));
        when(projectMemberRepo.findMemberIdsByProject(projectId))
                .thenReturn(java.util.List.of("m1", "m2")); // m3 não está alocado

        service.removeMemberAllocation(projectId, "m3");

        verify(projectMemberRepo, never())
                .deleteByProjectIdAndMember(anyLong(), anyString());
    }

    @Test
    @DisplayName("removeMemberAllocation: deve remover quando há mais de 1 membro alocado")
    void removeAllocation_shouldRemove_whenMoreThanOneAllocated() {
        long projectId = 9L;

        var p = new Project();
        p.setId(projectId);
        p.setStatus(ProjectStatus.EM_ANALISE);

        when(projectRepo.findById(projectId)).thenReturn(Optional.of(p));
        when(projectMemberRepo.findMemberIdsByProject(projectId))
                .thenReturn(java.util.List.of("m1", "m2")); // >1

        service.removeMemberAllocation(projectId, "m2");

        verify(projectMemberRepo).deleteByProjectIdAndMember(projectId, "m2");
    }

    @Test
    @DisplayName("getById: deve lançar NotFoundException quando o projeto não existir")
    void getById_shouldThrowNotFound_whenMissing() {
        when(projectRepo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Project not found");
    }

    @Test
    @DisplayName("delete: deve deletar quando status permitir (ex.: CANCELADO)")
    void delete_shouldDelete_whenStatusAllows() {
        long id = 100L;

        var p = new Project();
        p.setId(id);
        p.setStatus(ProjectStatus.CANCELADO);

        when(projectRepo.findById(id)).thenReturn(Optional.of(p));

        service.delete(id);

        verify(projectRepo).delete(p);
    }

    @Test
    @DisplayName("update: deve falhar ao trocar gerente para alguém que não é GERENTE")
    void update_shouldFail_whenChangingManagerToNonGerente() {
        long id = 123L;

        var original = new Project();
        original.setId(id);
        original.setName("P");
        original.setStartDate(LocalDate.of(2025, 1, 1));
        original.setExpectedEndDate(LocalDate.of(2025, 3, 1));
        original.setTotalBudget(new BigDecimal("1000"));
        original.setManagerExternalId("mgr-1");
        original.setStatus(ProjectStatus.EM_ANALISE);
        when(projectRepo.findById(id)).thenReturn(Optional.of(original));

        var dto = new ProjectCreateRequest(
                "P",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 3, 1),
                null,
                new BigDecimal("1000"),
                "desc",
                "func-9"
        );

        when(memberClient.getById("func-9"))
                .thenReturn(Optional.of(new ExternalMemberDTO("func-9", "Fulano", "FUNCIONARIO")));

        assertThatThrownBy(() -> service.update(id, dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("MANAGER role");

        verify(projectRepo, never()).save(any(Project.class));
    }
    
}