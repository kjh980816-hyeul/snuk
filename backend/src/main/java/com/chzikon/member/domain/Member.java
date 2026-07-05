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

    @Column(name = "chzzk_channel_id", nullable = false, unique = true, length = 128)
    private String chzzkChannelId;

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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Member(String chzzkChannelId, String nickname, String profileImageUrl,
                   Integer followerCount, Role role) {
        this.chzzkChannelId = chzzkChannelId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.followerCount = followerCount;
        this.role = role;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Member create(String chzzkChannelId, String nickname, String profileImageUrl,
                                Integer followerCount, Role role) {
        return new Member(chzzkChannelId, nickname, profileImageUrl, followerCount, role);
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

    /** 치지직 프사로 복원(동기화 재개). */
    public void resetProfileImage(String chzzkImageUrl) {
        this.profileImageUrl = chzzkImageUrl;
        this.profileImageOverridden = false;
        this.updatedAt = LocalDateTime.now();
    }
}
