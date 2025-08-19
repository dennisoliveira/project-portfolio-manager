package com.github.dennisoliveira.portfolio.service.domain;

import com.github.dennisoliveira.portfolio.domain.ProjectStatus;
import com.github.dennisoliveira.portfolio.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatusTransitionValidatorTest {

    private final StatusTransitionValidator validator = new StatusTransitionValidator();

    @Test
    @DisplayName("Deve permitir todas as transições sequenciais válidas")
    void shouldAllowSequentialTransitions() {
        // em análise -> análise realizada
        assertThatCode(() -> validator.validate(ProjectStatus.EM_ANALISE, ProjectStatus.ANALISE_REALIZADA))
                .doesNotThrowAnyException();
        // análise realizada -> análise aprovada
        assertThatCode(() -> validator.validate(ProjectStatus.ANALISE_REALIZADA, ProjectStatus.ANALISE_APROVADA))
                .doesNotThrowAnyException();
        // análise aprovada -> iniciado
        assertThatCode(() -> validator.validate(ProjectStatus.ANALISE_APROVADA, ProjectStatus.INICIADO))
                .doesNotThrowAnyException();
        // iniciado -> planejado
        assertThatCode(() -> validator.validate(ProjectStatus.INICIADO, ProjectStatus.PLANEJADO))
                .doesNotThrowAnyException();
        // planejado -> em andamento
        assertThatCode(() -> validator.validate(ProjectStatus.PLANEJADO, ProjectStatus.EM_ANDAMENTO))
                .doesNotThrowAnyException();
        // em andamento -> encerrado
        assertThatCode(() -> validator.validate(ProjectStatus.EM_ANDAMENTO, ProjectStatus.ENCERRADO))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve rejeitar pular etapas (transições inválidas)")
    void shouldRejectSkippingSteps() {
        // em análise -> iniciado (pulo)
        assertThrows(BusinessRuleException.class, () ->
                validator.validate(ProjectStatus.EM_ANALISE, ProjectStatus.INICIADO));

        // análise aprovada -> em andamento (pulo)
        assertThrows(BusinessRuleException.class, () ->
                validator.validate(ProjectStatus.ANALISE_APROVADA, ProjectStatus.EM_ANDAMENTO));

        // planejado -> encerrado (pulo)
        assertThrows(BusinessRuleException.class, () ->
                validator.validate(ProjectStatus.PLANEJADO, ProjectStatus.ENCERRADO));
    }

    @Test
    @DisplayName("Cancelado pode ser aplicado a partir de qualquer status")
    void shouldAllowCancelFromAnyStatus() {
        for (ProjectStatus current : ProjectStatus.values()) {
            assertThatCode(() -> validator.validate(current, ProjectStatus.CANCELADO))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("ENCERRADO e CANCELADO não podem transitar para outro status (exceto cancelar, que é sempre permitido)")
    void terminalStatesShouldRejectNonCancelTransitions() {
        for (ProjectStatus next : ProjectStatus.values()) {
            if (next == ProjectStatus.CANCELADO) continue; // já coberto acima

            // ENCERRADO -> qualquer outro
            assertThrows(BusinessRuleException.class, () ->
                    validator.validate(ProjectStatus.ENCERRADO, next));

            // CANCELADO -> qualquer outro
            assertThrows(BusinessRuleException.class, () ->
                    validator.validate(ProjectStatus.CANCELADO, next));
        }
    }

    @Test
    @DisplayName("Mensagens de erro devem indicar a transição inválida (DX)")
    void errorMessageShouldMentionTransition() {
        var ex = assertThrows(BusinessRuleException.class, () ->
                validator.validate(ProjectStatus.EM_ANALISE, ProjectStatus.INICIADO));
        // mensagem esperada: "Invalid status transition: EM_ANALISE -> INICIADO"
        // Não assertamos exatamente igual para evitar fragilidade; apenas checamos partes relevantes.
        org.assertj.core.api.Assertions.assertThat(ex.getMessage())
                .contains("Invalid status transition", "EM_ANALISE", "INICIADO");
    }
}