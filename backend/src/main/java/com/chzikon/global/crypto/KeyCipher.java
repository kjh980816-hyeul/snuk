package com.chzikon.global.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 게임 키 암호화 (AES-256-GCM).
 * - 평문 키는 입력 즉시 암호화하여 저장(security.md). 복호화는 배정된 본인 노출 시점에만.
 * - 중복 감지는 평문 비교 대신 fingerprint(SHA-256)로 처리하여 평문을 메모리에 오래 두지 않음.
 */
@Component
public class KeyCipher {

    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;       // GCM 권장 nonce 길이
    private static final int TAG_BITS = 128;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public KeyCipher(@Value("${app.key-encryption.secret}") String base64Secret) {
        byte[] raw = Base64.getDecoder().decode(base64Secret);
        if (raw.length != 32) {
            throw new IllegalStateException(
                    "KEY_ENCRYPTION_SECRET must be 32 bytes (base64). got=" + raw.length);
        }
        this.secretKey = new SecretKeySpec(raw, "AES");
    }

    @PostConstruct
    void verify() {
        // 부팅 시 라운드트립 1회로 키 설정 검증 (잘못된 키로 운영 진입 방지)
        String probe = "chzikon-key-cipher-selftest";
        if (!probe.equals(decrypt(encrypt(probe)))) {
            throw new IllegalStateException("KeyCipher self-test failed");
        }
    }

    /** 평문 → base64(iv || ciphertext+tag) */
    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("key encrypt failed", e);
        }
    }

    public String decrypt(String enc) {
        try {
            byte[] all = Base64.getDecoder().decode(enc);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            byte[] ct = new byte[all.length - IV_LENGTH];
            System.arraycopy(all, IV_LENGTH, ct, 0, ct.length);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("key decrypt failed", e);
        }
    }

    /** 중복 감지용 결정적 해시 (캠페인 단위 unique 인덱스에 사용). 평문 비노출. */
    public String fingerprint(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(h);
        } catch (Exception e) {
            throw new IllegalStateException("fingerprint failed", e);
        }
    }

    /** 어드민/응답 노출용 마스킹: 앞 4자리만, 나머지는 점. */
    public static String mask(String plain) {
        if (plain == null || plain.isEmpty()) {
            return "";
        }
        String head = plain.length() <= 4 ? plain : plain.substring(0, 4);
        return head + "-••••";
    }
}
