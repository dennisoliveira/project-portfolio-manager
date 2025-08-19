package com.github.dennisoliveira.portfolio.repository;

import com.github.dennisoliveira.portfolio.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {
    @Query("select p.status as status, count(p) as qty from Project p group by p.status")
    List<Object[]> countByStatus();

    @Query("select p.status as status, coalesce(sum(p.totalBudget), 0) as total from Project p group by p.status")
    List<Object[]> sumBudgetByStatus();

    @Query("""
       select p.startDate, p.actualEndDate
       from Project p
       where p.status = com.github.dennisoliveira.portfolio.domain.ProjectStatus.ENCERRADO
         and p.actualEndDate is not null
    """)
    List<Object[]> findClosedProjectDates();

    @Query(value = "select count(distinct pm.member_id) from project_member pm", nativeQuery = true)
    Long countDistinctMembersAllocated();
}
