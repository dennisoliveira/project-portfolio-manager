package com.github.dennisoliveira.portfolio.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_member")
@Getter @Setter
@NoArgsConstructor
public class ProjectMember {

    @EmbeddedId
    private ProjectMemberId id;

    @MapsId("projectId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public ProjectMember(Project project, String memberExternalId) {
        this.project = project;
        this.id = new ProjectMemberId(project.getId(), memberExternalId);
    }
}