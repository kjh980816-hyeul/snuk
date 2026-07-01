package com.chzikon.global.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeyCipherTest {

    // 로컬 기본 키와 동일한 32바이트 base64
    private final KeyCipher cipher = new KeyCipher("bG9jYWwtZGV2LTMyYnl0ZS1rZXktMDAwMDAwMDAwMDA=");

    @Test
    void encrypt_decrypt_roundtrip() {
        String plain = "ABCD-EFGH-IJKL-MNOP";
        String enc = cipher.encrypt(plain);
        assertThat(enc).isNotEqualTo(plain);     // 암호문은 평문과 다름
        assertThat(cipher.decrypt(enc)).isEqualTo(plain);
    }

    @Test
    void encrypt_is_nondeterministic_due_to_iv() {
        String plain = "SAME-KEY-VALUE";
        assertThat(cipher.encrypt(plain)).isNotEqualTo(cipher.encrypt(plain));
    }

    @Test
    void fingerprint_is_deterministic_and_hides_plaintext() {
        String plain = "DUP-KEY-1234";
        String fp1 = cipher.fingerprint(plain);
        String fp2 = cipher.fingerprint(plain);
        assertThat(fp1).isEqualTo(fp2);          // 중복 감지에 사용 가능
        assertThat(fp1).doesNotContain(plain);   // 평문 비노출
    }

    @Test
    void mask_exposes_only_prefix() {
        assertThat(KeyCipher.mask("ABCDEFGH")).isEqualTo("ABCD-••••");
        assertThat(KeyCipher.mask("")).isEmpty();
    }
}
