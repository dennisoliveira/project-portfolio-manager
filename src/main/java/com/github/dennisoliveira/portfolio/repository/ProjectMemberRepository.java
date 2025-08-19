package com.github.dennisoliveira.portfolio.repository;

import com.github.dennisoliveira.portfolio.domain.ProjectMember;
import com.github.dennisoliveira.portfolio.domain.ProjectMemberId;
import com.github.dennisoliveira.portfolio.domain.ProjectStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

    @Query("""
      select pm.id.memberExternalId
      from ProjectMember pm
      where pm.project.id = :pid
      order by pm.id.memberExternalId
    """)
    List<String> findMemberIdsByProject(@Param("pid") Long projectId);

    @Query("""
      select count(distinct pm.project.id)
      from ProjectMember pm
      where pm.id.memberExternalId = :mid
        and pm.project.status not in :closed
    """)
    long countActiveProjectsForMember(@Param("mid") String memberExternalId,
                                      @Param("closed") Collection<ProjectStatus> closed);

    boolean existsById(ProjectMemberId id);

    long countByProject_Id(Long projectId);

    @Modifying
    @Query("delete from ProjectMember pm where pm.id.projectId = :pid and pm.id.memberExternalId = :mid")
    void deleteByProjectIdAndMember(@Param("pid") Long projectId, @Param("mid") String memberExternalId);
}