package com.chzikon.member.repository;

import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Provider;
import com.chzikon.member.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByProviderAndChannelId(Provider provider, String channelId);

    boolean existsByRole(Role role);

    /** 공개 스트리머 목록 — 팔로워 많은 순 상위 60명. */
    List<Member> findTop60ByRoleOrderByFollowerCountDesc(Role role);
}
