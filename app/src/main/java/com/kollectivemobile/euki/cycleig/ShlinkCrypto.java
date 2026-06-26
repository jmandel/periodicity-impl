package com.kollectivemobile.euki.cycleig;

import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ShlinkCrypto {
    private static final int KEY_BITS = 256;
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public EncryptedPayload encrypt(String plaintext) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(KEY_BITS);
            SecretKey key = keyGenerator.generateKey();
            return new EncryptedPayload(encryptCompact(plaintext, key.getEncoded()), SmartHealthLink.b64Url(key.getEncoded()));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not encrypt Cycle IG Bundle.", exception);
        }
    }

    public String encryptCompact(String plaintext, byte[] keyBytes) {
        try {
            JsonObject header = new JsonObject();
            header.addProperty("alg", "dir");
            header.addProperty("enc", "A256GCM");
            header.addProperty("cty", "application/fhir+json");
            String protectedHeader = SmartHealthLink.b64Url(header.toString().getBytes(StandardCharsets.UTF_8));

            byte[] nonce = new byte[NONCE_BYTES];
            SECURE_RANDOM.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(TAG_BYTES * 8, nonce));
            cipher.updateAAD(protectedHeader.getBytes(StandardCharsets.US_ASCII));
            byte[] sealed = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext = Arrays.copyOfRange(sealed, 0, sealed.length - TAG_BYTES);
            byte[] tag = Arrays.copyOfRange(sealed, sealed.length - TAG_BYTES, sealed.length);

            return protectedHeader + ".." + SmartHealthLink.b64Url(nonce) + "." + SmartHealthLink.b64Url(ciphertext) + "." + SmartHealthLink.b64Url(tag);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not encrypt Cycle IG Bundle.", exception);
        }
    }

    public String decryptCompact(String jwe, String keyB64) {
        try {
            String[] parts = jwe.trim().split("\\.", -1);
            if (parts.length != 5 || !parts[1].isEmpty()) {
                throw new IllegalArgumentException("Malformed compact JWE.");
            }

            byte[] ciphertext = SmartHealthLink.b64UrlDecode(parts[3]);
            byte[] tag = SmartHealthLink.b64UrlDecode(parts[4]);
            byte[] sealed = new byte[ciphertext.length + tag.length];
            System.arraycopy(ciphertext, 0, sealed, 0, ciphertext.length);
            System.arraycopy(tag, 0, sealed, ciphertext.length, tag.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(SmartHealthLink.b64UrlDecode(keyB64), "AES"), new GCMParameterSpec(TAG_BYTES * 8, SmartHealthLink.b64UrlDecode(parts[2])));
            cipher.updateAAD(parts[0].getBytes(StandardCharsets.US_ASCII));
            return new String(cipher.doFinal(sealed), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not decrypt compact JWE.", exception);
        }
    }

    public static class EncryptedPayload {
        public final String jwe;
        public final String key;

        EncryptedPayload(String jwe, String key) {
            this.jwe = jwe;
            this.key = key;
        }
    }
}
