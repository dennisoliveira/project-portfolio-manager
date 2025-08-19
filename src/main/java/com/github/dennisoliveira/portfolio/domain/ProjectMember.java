package com.github.dennisoliveira.portfolio.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "project_member")
public class ProjectMember {

    @EmbeddedId
    private ProjectMemberId id;

    @MapsId("projectId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // opcional: facilidade de acesso (mesmo valor do ID, só leitura)
    @Column(name = "member_external_id", nullable = false, length = 100, insertable = false, updatable = false)
    private String memberExternalId;

    protected ProjectMember() {}

    public ProjectMember(Project project, String memberExternalId) {
        this.project = project;
        this.id = new ProjectMemberId(project.getId(), memberExternalId);
        this.memberExternalId = memberExternalId;
    }

    public ProjectMemberId getId() { return id; }
    public Project getProject() { return project; }
    public String getMemberExternalId() { return memberExternalId; }
}