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
    public TournamentParticipant apply(Long tournamentId, Long memberId,
                                       List<com.chzikon.tournament.dto.ApplyFormJson.ApplyAnswer> answers) {
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
        // 질문 검증(항목 17) — 필수 질문은 텍스트나 이미지 중 하나는 있어야 함. 선택 질문은 비워도 됨.
        var questions = com.chzikon.tournament.dto.ApplyFormJson.questionsFromJson(tournament.getApplyQuestions());
        if (!questions.isEmpty()) {
            if (answers == null || answers.size() != questions.size()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "참가 신청 질문 답변이 누락됐습니다.");
            }
            for (int i = 0; i < questions.size(); i++) {
                var a = answers.get(i);
                if (questions.get(i).required() && (a == null || a.isBlank())) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT,
                            "필수 질문에 모두 답변해주세요: " + questions.get(i).q());
                }
            }
        }

        int followerSnapshot = member.getFollowerCount() != null ? member.getFollowerCount() : 0;
        try {
            return participantRepository.save(
                    new TournamentParticipant(tournamentId, memberId, followerSnapshot,
                            com.chzikon.tournament.dto.ApplyFormJson.answersToJson(answers)));
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
        List<TournamentParticipant> all =
                participantRepository.findByTournamentIdOrderByAppliedAtAsc(tournamentId);
        Map<Long, Member> members = memberRepository.findAllById(
                        all.stream().map(TournamentParticipant::getMemberId).distinct().toList())
                .stream().collect(Collectors.toMap(Member::getId, Function.identity()));
        return all.stream()
                .map(p -> ParticipantAdminView.of(p, members.get(p.getMemberId())))
                .toList();
    }

    /**
     * 참가 신청 내역 CSV(엑셀 호환) — 주최자(소유 스트리머) 또는 ADMIN (항목 18).
     * 헤더: 닉네임, 회원번호, 상태, 팔로워, 신청일 + 대회 질문들.
     */
    @Transactional(readOnly = true)
    public String exportCsv(Long tournamentId, Long memberId) {
        requireOwnedTournament(tournamentId, memberId);
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        var questions = com.chzikon.tournament.dto.ApplyFormJson.questionsFromJson(tournament.getApplyQuestions());
        List<com.chzikon.tournament.dto.ParticipantAdminView> rows = listParticipants(tournamentId);

        StringBuilder sb = new StringBuilder();
        sb.append(csvRow(java.util.stream.Stream.concat(
                java.util.stream.Stream.of("닉네임", "회원번호", "상태", "팔로워", "신청일"),
                questions.stream().map(q -> q.q() + (q.required() ? "" : " (선택)"))).toList()));
        for (var r : rows) {
            java.util.List<String> cells = new java.util.ArrayList<>(List.of(
                    r.nickname(), String.valueOf(r.memberId()), r.status(),
                    String.valueOf(r.followerSnapshot()),
                    r.appliedAt() != null ? r.appliedAt().toString() : ""));
            for (int i = 0; i < questions.size(); i++) {
                if (i < r.answers().size() && r.answers().get(i) != null) {
                    var a = r.answers().get(i);
                    String cell = a.text() != null ? a.text() : "";
                    if (a.imageUrl() != null && !a.imageUrl().isBlank()) {
                        cell = (cell.isBlank() ? "" : cell + " ") + "[사진] " + a.imageUrl();
                    }
                    cells.add(cell);
                } else {
                    cells.add("");
                }
            }
            sb.append(csvRow(cells));
        }
        return sb.toString();
    }

    private static String csvRow(List<String> cells) {
        return cells.stream()
                .map(c -> '"' + (c == null ? "" : c.replace("\"", "\"\"")) + '"')
                .collect(Collectors.joining(",")) + "\r\n";
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
