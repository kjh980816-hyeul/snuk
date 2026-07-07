package com.chzikon.member.domain;

/** 로그인 플랫폼. member 는 (provider, channel_id) 복합으로 식별한다. */
public enum Provider {
    CHZZK, CIME, SOOP;

    /** URL 경로 세그먼트(chzzk/cime/soop) → enum. 미지원 값은 null. */
    public static Provider fromPath(String value) {
        if (value == null) {
            return null;
        }
        for (Provider p : values()) {
            if (p.name().equalsIgnoreCase(value)) {
                return p;
            }
        }
        return null;
    }
}
