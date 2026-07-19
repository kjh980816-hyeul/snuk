package com.chzikon.tournament.service;

import com.chzikon.admin.service.AdminLogService;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.member.domain.Member;
import com.chzikon.member.repository.MemberRepository;
import com.chzikon.member.service.MemberService;
import com.chzikon.tournament.domain.Tournament;
import com.chzikon.tournament.domain.TournamentParticipant;
import com.chzikon.tournament.dto.MyParticipationResponse;
import com.chzikon.tournament.dto.ParticipantAdminView;
import com.chzikon.tournament.dto.ParticipantPublicView;
import com.chzikon.tournament.repository.TournamentParticipantRepository;
import com.chzikon.tournament.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TournamentParticipantService {

    private final TournamentRepository tournamentRepository;
    private final TournamentParticipantRepository participantRepository;
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final AdminLogService adminLogService;

    /**
     * 대회 참가 신청 — 승인제(캠페인 APPROVAL 패턴).
     * - 권한 STREAMER+ 백엔드 재검증(프론트 가드 불신뢰).
     * - 1인 1신청(unique). 접수는 PENDING, 정원 차감은 어드민 승인 시점.
     */
    @Transactional
    public TournamentParticipant apply(Long tournamentId, Long memberId) {
        Member member = memberService.getById(memberId);
        if (!member.getRole().isStreamerOrAbove()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_ROLE);
        }
        if (participantRepository.existsByTournamentIdAndMemberId(tournamentId, memberId)) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED);
        }

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!tournament.isOpenForApply()) {
            throw new BusinessException(ErrorCode.TOURNAMENT_NOT_OPEN);
        }

        int followerSnapshot = member.getFollowerCount() != null ? member.getFollowerCount() : 0;
        try {
            return participantRepository.save(
                    new TournamentParticipant(tournamentId, memberId, followerSnapshot));
        } catch (DataIntegrityViolationException e) {
            // unique(tournament_id, member_id) 동시 중복신청 방어
            throw new BusinessException(ErrorCode.ALREADY_JOINED);
        }
    }

    // ---------- 어드민 승인/거절 ----------

    /** 승인 — 정원은 락을 잡고 원자 차감(초과 시 TOURNAMENT_FULL, 트랜잭션 롤백). */
    @Transactional
    public void approve(Long participantId, Long actorId) {
        TournamentParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (participant.isApproved()) {
            return;
        }
        Tournament tournament = tournamentRepository.findByIdForUpdate(participant.getTournamentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        tournament.fillOneSlot();
        participant.approve();
        adminLogService.record(actorId, "PARTICIPANT_APPROVE", "tournament_participant", participantId,
                "tournament=" + tournament.getId() + " member=" + participant.getMemberId());
    }

    @Transactional
    public void reject(Long participantId, Long actorId) {
        TournamentParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        participant.reject();
        adminLogService.record(actorId, "PARTICIPANT_REJECT", "tournament_participant", participantId,
                "member=" + participant.getMemberId());
    }

    @Transactional(readOnly = true)
    public List<ParticipantAdminView> listParticipants(Long tournamentId) {
        return participantRepository.findByTournamentIdOrderByAppliedAtAsc(tournamentId).stream()
                .map(ParticipantAdminView::from)
                .toList();
    }

    /** 공개 로스터 — 승인된 참가자만(닉네임·프사). 대진표/참여 스트리머 표시용. */
    @Transactional(readOnly = true)
    public List<ParticipantPublicView> listApprovedPublic(Long tournamentId) {
        List<TournamentParticipant> approved =
                participantRepository.findByTournamentIdOrderByAppliedAtAsc(tournamentId).stream()
                        .filter(TournamentParticipant::isApproved)
                        .toList();
        Map<Long, Member> members = memberRepository.findAllById(
                        approved.stream().map(TournamentParticipant::getMemberId).distinct().toList())
                .stream().collect(Collectors.toMap(Member::getId, Function.identity()));
        return approved.stream()
                .map(p -> ParticipantPublicView.of(p, members.get(p.getMemberId())))
                .toList();
    }

    // ---------- 주최자(소유 스트리머/ADMIN) 참가자 관리 ----------

    private void requireOwnedTournament(Long tournamentId, Long memberId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Member member = memberService.getById(memberId);
        if (!tournament.isOwnedBy(memberId)
                && member.getRole() != com.chzikon.member.domain.Role.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    @Transactional(readOnly = true)
    public List<com.chzikon.tournament.dto.ParticipantManageView> listForManage(Long tournamentId, Long memberId) {
        requireOwnedTournament(tournamentId, memberId);
        List<TournamentParticipant> all =
                participantRepository.findByTournamentIdOrderByAppliedAtAsc(tournamentId);
        Map<Long, Member> members = memberRepository.findAllById(
                        all.stream().map(TournamentParticipant::getMemberId).distinct().toList())
                .stream().collect(Collectors.toMap(Member::getId, Function.identity()));
        return all.stream()
                .map(p -> com.chzikon.tournament.dto.ParticipantManageView.of(p, members.get(p.getMemberId())))
                .toList();
    }

    /** 주최자 승인/거절 — 본인 대회의 참가자인지 검증 후 기존 승인 로직(비관락 정원 차감) 재사용. */
    @Transactional
    public void decideByOwner(Long tournamentId, Long participantId, boolean approveIt, Long memberId) {
        requireOwnedTournament(tournamentId, memberId);
        TournamentParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!participant.getTournamentId().equals(tournamentId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND); // 남의 대회 참가자 id 우회 차단
        }
        if (approveIt) {
            approve(participantId, memberId);
        } else {
            reject(participantId, memberId);
        }
    }

    // ---------- 본인 조회 ----------

    @Transactional(readOnly = true)
    public Optional<MyParticipationResponse> getMyParticipation(Long tournamentId, Long memberId) {
        return participantRepository.findByTournamentIdAndMemberId(tournamentId, memberId)
                .map(MyParticipationResponse::from);
    }
}
