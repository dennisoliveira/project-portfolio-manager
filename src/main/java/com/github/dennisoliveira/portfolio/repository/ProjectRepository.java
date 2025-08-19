package com.github.dennisoliveira.portfolio.repository;

import com.github.dennisoliveira.portfolio.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    @Query("""
      select p.status, count(p), coalesce(sum(p.totalBudget), 0)
      from Project p
      group by p.status
    """)
    List<Object[]> aggregateByStatusRaw();

    @Query(value = """
        select coalesce(
                 avg((p.actual_end_date - p.start_date))::float8,
                 0
               )
        from project p
        where p.status = 'ENCERRADO'
          and p.actual_end_date is not null
    """, nativeQuery = true)
    Double avgDurationDaysClosedProjects();

    @Query(value = "select count(distinct pm.member_external_id) from project_member pm", nativeQuery = true)
    Long countDistinctMembersAllocated();

    @Query(value = """
        select count(distinct pm.member_external_id)
        from project_member pm
        join project p on p.id = pm.project_id
        where p.status not in ('ENCERRADO','CANCELADO')
    """, nativeQuery = true)
    Long countDistinctMembersAllocatedActive();
}