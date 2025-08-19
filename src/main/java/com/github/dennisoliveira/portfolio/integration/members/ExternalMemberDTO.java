package com.github.dennisoliveira.portfolio.integration.members;

public record ExternalMemberDTO(
        String id,
        String name,
        String role
) {
    public boolean isFuncionario() {
        return "FUNCIONARIO".equalsIgnoreCase(role);
    }
    public boolean isGerente() {
        return "GERENTE".equalsIgnoreCase(role);
    }
}