package com.chzikon.member.repository;

import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByChzzkChannelId(String chzzkChannelId);

    boolean existsByRole(Role role);
}
