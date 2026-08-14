package com.chzikon.grant.service;

import com.chzikon.admin.service.AdminLogService;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.grant.domain.RoleRequest;
import com.chzikon.grant.repository.RoleRequestRepository;
import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Role;
import com.chzikon.member.service.MemberService;
import com.chzikon.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoleRequestService {

    private final RoleRequestRepository repository;
    private final MemberService memberService;
    private final NotificationService notificationService;
    private final AdminLogService adminLogService;
    private final com.chzikon.member.repository.MemberRepository memberRepository;

    /** 신청 — 이미 STREAMER 이상이면 불필요, 대기 중 신청 1건 제한. */
    @Transactional
    public RoleRequest apply(Long memberId, String message) {
        Member member = memberService.getById(memberId);
        if (member.getRole().isStreamerOrAbove()) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 스트리머 이상 권한입니다.");
        }
        if (repository.existsByMemberIdAndStatus(memberId, RoleRequest.Status.PENDING)) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 접수된 신청이 있습니다. 운영자 확인을 기다려주세요.");
        }
        return repository.save(new RoleRequest(memberId, message));
    }

    @Transactional(readOnly = true)
    public Optional<RoleRequest> myLatest(Long memberId) {
        return repository.findTopByMemberIdOrderByCreatedAtDesc(memberId);
    }

    // ---------- 어드민 ----------

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listForAdmin() {
        List<RoleRequest> requests = repository.findAllByOrderByCreatedAtDesc();
        var members = memberRepository.findAllById(
                        requests.stream().map(RoleRequest::getMemberId).distinct().toList())
                .stream().collect(java.util.stream.Collectors.toMap(Member::getId, m -> m));
        return requests.stream().map(r -> {
            Member m = members.get(r.getMemberId());
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("requestId", r.getId());
            row.put("memberId", r.getMemberId());
            row.put("nickname", m != null ? m.getNickname() : ("회원#" + r.getMemberId()));
            row.put("profileImageUrl", m != null ? m.getProfileImageUrl() : null);
            row.put("provider", m != null ? m.getProvider().name() : null);
            row.put("followerCount", m != null ? m.getFollowerCount() : null);
            row.put("currentRole", m != null ? m.getRole().name() : null);
            row.put("message", r.getMessage());
            row.put("status", r.getStatus().name());
            row.put("createdAt", r.getCreatedAt());
            row.put("decidedAt", r.getDecidedAt());
            return row;
        }).toList();
    }

    /** 승인 — 기존 role-override 메커니즘 재사용(STREAMER 고정 승격) + 알림. */
    @Transactional
    public void approve(Long requestId, Long actorId) {
        RoleRequest request = repository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!request.isPending()) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 처리된 신청입니다.");
        }
        memberService.overrideRole(request.getMemberId(), Role.STREAMER);
        request.approve(actorId);
        notificationService.notify(request.getMemberId(), "ROLE_APPROVED",
                "스트리머 권한 신청이 승인됐습니다",
                "이제 컨텐츠·체험단·대회에 신청할 수 있어요. (재로그인 시 전체 반영)", "/mypage");
        adminLogService.record(actorId, "ROLE_REQUEST_APPROVE", "role_request", requestId,
                "member=" + request.getMemberId());
    }

    @Transactional
    public void reject(Long requestId, Long actorId) {
        RoleRequest request = repository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!request.isPending()) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 처리된 신청입니다.");
        }
        request.reject(actorId);
        notificationService.notify(request.getMemberId(), "ROLE_REJECTED",
                "스트리머 권한 신청이 반려됐습니다",
                "자세한 내용은 운영자에게 문의해주세요.", null);
        adminLogService.record(actorId, "ROLE_REQUEST_REJECT", "role_request", requestId,
                "member=" + request.getMemberId());
    }
}
