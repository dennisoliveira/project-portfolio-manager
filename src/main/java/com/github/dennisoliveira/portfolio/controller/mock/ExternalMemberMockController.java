package com.github.dennisoliveira.portfolio.controller.mock;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/external/members")
@Tag(name = "Member (Mock external members)")
public class ExternalMemberMockController {

    private final Map<String, ExternalMemberDTO> store = new ConcurrentHashMap<>();

    public static final String SEED_MANAGER_ID     = "00000000-0000-0000-0000-000000000001";
    public static final String SEED_EMPLOYEE1_ID   = "00000000-0000-0000-0000-000000000002";
    public static final String SEED_EMPLOYEE2_ID   = "00000000-0000-0000-0000-000000000003";

    public enum ExternalRole {
        FUNCIONARIO, GERENTE;

        public static ExternalRole parse(String s) {
            if (s == null || s.isBlank()) {
                throw new IllegalArgumentException("role is required");
            }
            String norm = Normalizer.normalize(s, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .trim()
                    .toUpperCase(Locale.ROOT);

            return switch (norm) {
                case "FUNCIONARIO" -> FUNCIONARIO;
                case "GERENTE"     -> GERENTE;
                default -> throw new IllegalArgumentException("only accepted values: FUNCIONARIO, GERENTE");
            };
        }
    }

    public record CreateMemberRequest(@NotBlank String name, @NotBlank String role) {}
    public record ExternalMemberDTO(String id, String name, ExternalRole role) {}

    @PostConstruct
    void seedMembers() {
        store.put(SEED_MANAGER_ID, new ExternalMemberDTO(SEED_MANAGER_ID, "Alice Manager", ExternalRole.GERENTE));
        store.put(SEED_EMPLOYEE1_ID, new ExternalMemberDTO(SEED_EMPLOYEE1_ID, "Bob Employee", ExternalRole.FUNCIONARIO));
        store.put(SEED_EMPLOYEE2_ID, new ExternalMemberDTO(SEED_EMPLOYEE2_ID, "Carol Employee", ExternalRole.FUNCIONARIO));
    }

    @Operation(summary = "Criar um novo usuário externo")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateMemberRequest req) {
        final ExternalRole role;
        try {
            role = ExternalRole.parse(req.role());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.unprocessableEntity()
                    .body(Map.of(
                            "error", "INVALID_ROLE",
                            "message", e.getMessage(),
                            "accepted", new String[]{"FUNCIONARIO", "GERENTE"}
                    ));
        }

        String id = UUID.randomUUID().toString();
        ExternalMemberDTO dto = new ExternalMemberDTO(id, req.name().trim(), role);
        store.put(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Operation(summary = "Exibir usuário externo")
    @GetMapping("/{id}")
    public ResponseEntity<ExternalMemberDTO> get(@PathVariable String id) {
        ExternalMemberDTO dto = store.get(id);
        return (dto == null) ? ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                : ResponseEntity.ok(dto);
    }
}