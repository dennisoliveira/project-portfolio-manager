package com.github.dennisoliveira.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ProjectMemberId implements Serializable {

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "member_external_id", nullable = false, length = 100)
    private String memberExternalId;

    public ProjectMemberId() {}

    public ProjectMemberId(Long projectId, String memberExternalId) {
        this.projectId = projectId;
        this.memberExternalId = memberExternalId;
    }

    public Long getProjectId() { return projectId; }
    public String getMemberExternalId() { return memberExternalId; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectMemberId that)) return false;
        return Objects.equals(projectId, that.projectId)
                && Objects.equals(memberExternalId, that.memberExternalId);
    }
    @Override public int hashCode() {
        return Objects.hash(projectId, memberExternalId);
    }
}
