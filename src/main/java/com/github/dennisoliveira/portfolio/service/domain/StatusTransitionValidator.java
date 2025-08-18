package com.github.dennisoliveira.portfolio.service.domain;

import com.github.dennisoliveira.portfolio.domain.ProjectStatus;
import com.github.dennisoliveira.portfolio.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class StatusTransitionValidator {

    private final Map<ProjectStatus, Set<ProjectStatus>> allowed = new EnumMap<>(ProjectStatus.class);

    public StatusTransitionValidator() {
        allowed.put(ProjectStatus.EM_ANALISE, EnumSet.of(ProjectStatus.ANALISE_REALIZADA, ProjectStatus.CANCELADO));
        allowed.put(ProjectStatus.ANALISE_REALIZADA, EnumSet.of(ProjectStatus.ANALISE_APROVADA, ProjectStatus.CANCELADO));
        allowed.put(ProjectStatus.ANALISE_APROVADA, EnumSet.of(ProjectStatus.INICIADO, ProjectStatus.CANCELADO));
        allowed.put(ProjectStatus.INICIADO, EnumSet.of(ProjectStatus.PLANEJADO, ProjectStatus.CANCELADO));
        allowed.put(ProjectStatus.PLANEJADO, EnumSet.of(ProjectStatus.EM_ANDAMENTO, ProjectStatus.CANCELADO));
        allowed.put(ProjectStatus.EM_ANDAMENTO, EnumSet.of(ProjectStatus.ENCERRADO, ProjectStatus.CANCELADO));
        allowed.put(ProjectStatus.ENCERRADO, EnumSet.noneOf(ProjectStatus.class));
        allowed.put(ProjectStatus.CANCELADO, EnumSet.noneOf(ProjectStatus.class));
    }

    public void validate(ProjectStatus current, ProjectStatus next) {
        if (next == ProjectStatus.CANCELADO) return;
        var set = allowed.getOrDefault(current, Set.of());
        if (!set.contains(next)) {
            throw new BusinessRuleException("Invalid status transition: " + current + " -> " + next);
        }
    }
}