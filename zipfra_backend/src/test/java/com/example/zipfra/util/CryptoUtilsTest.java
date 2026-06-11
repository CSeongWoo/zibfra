package com.example.zipfra.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoUtilsTest {

    private CryptoUtils cryptoUtils;

    @BeforeEach
    void setUp() {
        cryptoUtils = new CryptoUtils();
        ReflectionTestUtils.setField(cryptoUtils, "aesKey", "12345678901234567890123456789012");
    }

    @Test
    void testMaskPii_PhoneNumbers() {
        String content = "My phone number is 010-1234-5678.";
        String masked = cryptoUtils.maskPii(content);
        assertThat(masked).isEqualTo("My phone number is 010-****-5678.");
    }

    @Test
    void testMaskPii_ResidentRegistrationNumbers() {
        String content = "Resident ID: 950101-1234567 and another 991231-2345678";
        String masked = cryptoUtils.maskPii(content);
        assertThat(masked).isEqualTo("Resident ID: 950101-******* and another 991231-*******");
    }

    @Test
    void testMaskPii_NullOrEmpty() {
        assertThat(cryptoUtils.maskPii(null)).isNull();
        assertThat(cryptoUtils.maskPii("")).isEmpty();
    }

    @Test
    void testEncryptDecrypt() {
        String original = "This is a secret message containing personal information.";
        byte[] encrypted = cryptoUtils.encrypt(original);
        assertThat(encrypted).isNotNull();
        assertThat(encrypted.length).isGreaterThan(16); // IV (16 bytes) + ciphertext

        String decrypted = cryptoUtils.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    void testEncrypt_Failure_InvalidKey() {
        ReflectionTestUtils.setField(cryptoUtils, "aesKey", "short-key");
        assertThatThrownBy(() -> cryptoUtils.encrypt("secret"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Encryption failed");
    }

    @Test
    void testDecrypt_Failure_InvalidData() {
        byte[] invalidData = new byte[10];
        assertThatThrownBy(() -> cryptoUtils.decrypt(invalidData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Decryption failed");
    }
}
