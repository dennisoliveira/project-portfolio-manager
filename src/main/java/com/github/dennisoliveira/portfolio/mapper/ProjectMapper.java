package com.github.dennisoliveira.portfolio.mapper;

import com.github.dennisoliveira.portfolio.domain.Project;
import com.github.dennisoliveira.portfolio.dto.ProjectCreateRequest;
import com.github.dennisoliveira.portfolio.dto.ProjectResponse;
import com.github.dennisoliveira.portfolio.service.domain.RiskClassifier;
import org.mapstruct.*;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Project toEntity(ProjectCreateRequest dto);

    @Mapping(target = "managerId", source = "manager.id")
    @Mapping(target = "risk",
            expression = "java(riskClassifier.classify(p.getTotalBudget(), p.getStartDate(), p.getExpectedEndDate()))")
    ProjectResponse toResponse(Project p, @Context RiskClassifier riskClassifier);

    List<ProjectResponse> toResponseList(List<Project> projects, @Context RiskClassifier riskClassifier);
}