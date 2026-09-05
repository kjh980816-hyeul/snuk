package com.chzikon.grant.repository;

import com.chzikon.grant.domain.RoleRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRequestRepository extends JpaRepository<RoleRequest, Long> {

    Optional<RoleRequest> findTopByMemberIdOrderByCreatedAtDesc(Long memberId);

    boolean existsByMemberIdAndStatus(Long memberId, RoleRequest.Status status);

    List<RoleRequest> findAllByOrderByCreatedAtDesc();
}
