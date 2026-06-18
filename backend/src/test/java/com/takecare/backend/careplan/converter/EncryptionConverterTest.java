package com.takecare.backend.careplan.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptionConverterTest {

    private EncryptionConverter converter;

    @BeforeEach
    void setUp() {
        converter = new EncryptionConverter();
        converter.setSecretKey("MySuperSecretTestKey1234567890!");
    }

    @Test
    void testEncryptDecryptSuccessful() {
        String originalText = "Nota médica confidencial del paciente.";
        
        // Encrypt
        String encrypted = converter.convertToDatabaseColumn(originalText);
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(originalText);
        
        // Decrypt
        String decrypted = converter.convertToEntityAttribute(encrypted);
        assertThat(decrypted).isEqualTo(originalText);
    }

    @Test
    void testNullHandling() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void testFallbackForPlaintext() {
        String plainText = "Nota antigua en texto plano no cifrada";
        
        // Since it's not encrypted, decrypting should fail and fallback to returning the original string.
        String decrypted = converter.convertToEntityAttribute(plainText);
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    void testFallbackForInvalidBase64() {
        String invalidBase64 = "!!!NotBase64!!!";
        
        // Since it's invalid base64, Base64 decoding will fail, falling back to returning the input string.
        String decrypted = converter.convertToEntityAttribute(invalidBase64);
        assertThat(decrypted).isEqualTo(invalidBase64);
    }
}
