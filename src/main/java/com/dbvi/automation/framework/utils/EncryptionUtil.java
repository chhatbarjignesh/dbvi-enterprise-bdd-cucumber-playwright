package com.dbvi.automation.framework.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 * EncryptionUtil provides Password-Based Encryption (PBE) with MD5 and DES. It is used to securely
 * encrypt and decrypt sensitive corporate credentials.
 */
public class EncryptionUtil {

    private static final char[] PASSWORD = "enfldsgbnlsngdlksdsgm".toCharArray();
    private static final byte[] SALT = {
        (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
        (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
    };

    /** Main method to generate encrypted strings for configurations. */
    public static void main(String[] args) {
        try {
            // Encrypt and print "ChangeMe123" to obtain the secure string
            String encrypted = encrypt("ChangeMe123");
            System.out.println("Encrypted 'ChangeMe123': " + encrypted);
            System.out.println("Decrypted test: " + decrypt(encrypted));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String encrypt(String property)
            throws GeneralSecurityException, UnsupportedEncodingException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
        Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
        pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
        return base64Encode(pbeCipher.doFinal(property.getBytes("UTF-8")));
    }

    public static String base64Encode(byte[] bytes) {
        return new String(Base64.getMimeEncoder().encode(bytes), StandardCharsets.UTF_8);
    }

    public static String decrypt(String property) throws GeneralSecurityException, IOException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
        Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
        pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
        String temp = new String(pbeCipher.doFinal(base64Decode(property)), "UTF-8");
        return temp;
    }

    private static byte[] base64Decode(String property) {
        Base64.Decoder mimeDecoder = Base64.getMimeDecoder();
        return mimeDecoder.decode(property);
    }
}
