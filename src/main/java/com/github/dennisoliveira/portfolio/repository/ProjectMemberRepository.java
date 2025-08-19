package com.github.dennisoliveira.portfolio.repository;

import com.github.dennisoliveira.portfolio.domain.ProjectMember;
import com.github.dennisoliveira.portfolio.domain.ProjectMemberId;
import com.github.dennisoliveira.portfolio.domain.ProjectStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

    @Query("select pm.id.memberExternalId from ProjectMember pm where pm.id.projectId = :projectId")
    List<String> findMemberIdsByProject(@Param("projectId") Long projectId);

    @Query("""
      select count(distinct pm.id.projectId)
      from ProjectMember pm
      where pm.id.memberExternalId = :memberId
        and pm.project.status not in :closedStatuses
    """)
    long countActiveProjectsForMember(@Param("memberId") String memberId,
                                      @Param("closedStatuses") Set<ProjectStatus> closedStatuses);

    @Modifying
    @Query("delete from ProjectMember pm where pm.id.projectId = :projectId and pm.id.memberExternalId = :memberId")
    void deleteByProjectIdAndMember(@Param("projectId") Long projectId, @Param("memberId") String memberId);
}
