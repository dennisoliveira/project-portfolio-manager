package com.github.dennisoliveira.portfolio.repository;

import com.github.dennisoliveira.portfolio.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByExternalId(String externalId);
}
