package com.github.dennisoliveira.portfolio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Members to allocate to a project")
public record AllocationRequest(
        @NotNull
        @Size(min = 1, max = 10, message = "You must provide between 1 and 10 memberExternalIds")
        @Schema(description = "List of external member IDs (UUIDs) to allocate", example = "[\"00000000-0000-0000-0000-000000000002\",\"00000000-0000-0000-0000-000000000003\"]")
        List<@NotBlank String> memberExternalIds
) {}
