package com.chzikon.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 플랫폼(치지직/씨미/숲). (provider, channel_id) 복합 유니크 — V7. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provider provider = Provider.CHZZK;

    @Column(name = "channel_id", nullable = false, length = 128)
    private String channelId;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(name = "profile_image_url", length = 512)
    private String profileImageUrl;

    @Column(name = "follower_count")
    private Integer followerCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.VIEWER;

    @Column(name = "role_overridden", nullable = false)
    private boolean roleOverridden = false;

    @Column(name = "profile_image_overridden", nullable = false)
    private boolean profileImageOverridden = false;

    /** 포인트 — 하루 첫 로그인 적립, 스포트라이트 등록 등에 사용(V15). */
    @Column(nullable = false)
    private int points = 0;

    @Column(name = "last_daily_point_at")
    private java.time.LocalDate lastDailyPointAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Member(Provider provider, String channelId, String nickname, String profileImageUrl,
                   Integer followerCount, Role role) {
        this.provider = provider;
        this.channelId = channelId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.followerCount = followerCount;
        this.role = role;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Member create(Provider provider, String channelId, String nickname,
                                String profileImageUrl, Integer followerCount, Role role) {
        return new Member(provider, channelId, nickname, profileImageUrl, followerCount, role);
    }

    /** 매 로그인 시 프로필/팔로워 갱신. role 자동 재산정은 오버라이드 안 된 경우만, 프사는 수동 변경 안 한 경우만. */
    public void refreshOnLogin(String nickname, String profileImageUrl, Integer followerCount, Role recomputedRole) {
        this.nickname = nickname;
        if (!this.profileImageOverridden) {
            this.profileImageUrl = profileImageUrl;
        }
        this.followerCount = followerCount;
        if (!this.roleOverridden && this.role != Role.ADMIN) {
            this.role = recomputedRole;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /** 하루 첫 로그인 포인트 적립 — 오늘 이미 받았으면 false. */
    public boolean grantDailyPoint(int amount, java.time.LocalDate today) {
        if (amount <= 0 || today.equals(this.lastDailyPointAt)) {
            return false;
        }
        this.points += amount;
        this.lastDailyPointAt = today;
        this.updatedAt = LocalDateTime.now();
        return true;
    }

    /** 포인트 차감 — 부족하면 예외. */
    public void spendPoints(int amount) {
        if (amount <= 0) {
            return;
        }
        if (this.points < amount) {
            throw new com.chzikon.global.error.BusinessException(
                    com.chzikon.global.error.ErrorCode.POINT_INSUFFICIENT);
        }
        this.points -= amount;
        this.updatedAt = LocalDateTime.now();
    }

    /** 대표 수동 오버라이드(승격/강등). 이후 자동 재산정 제외. */
    public void overrideRole(Role role) {
        this.role = role;
        this.roleOverridden = true;
        this.updatedAt = LocalDateTime.now();
    }

    /** 오버라이드 해제 → 자동 재산정 복귀. */
    public void clearOverride() {
        this.roleOverridden = false;
        this.updatedAt = LocalDateTime.now();
    }

    /** 프사 직접 변경. 이후 로그인 동기화가 덮어쓰지 않음. */
    public void changeProfileImage(String imageUrl) {
        this.profileImageUrl = imageUrl;
        this.profileImageOverridden = true;
        this.updatedAt = LocalDateTime.now();
    }

    /** 플랫폼 프사로 복원(동기화 재개). 플랫폼이 조회 미지원(숲)이면 null → 다음 로그인 때 채워짐. */
    public void resetProfileImage(String platformImageUrl) {
        this.profileImageUrl = platformImageUrl;
        this.profileImageOverridden = false;
        this.updatedAt = LocalDateTime.now();
    }
}
