package com.dbvi.automation.framework.utils;

import com.warrenstrange.googleauth.GoogleAuthenticator;

/**
 * TotpUtil is a utility class for generating Time-based One-Time Passwords (TOTP) for 2FA
 * (Two-Factor Authentication) login verification.
 */
public class TotpUtil {
    private static final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    /**
     * Generates a 6-digit Time-based One-Time Password (TOTP) code for the given Base32 secret key
     * at the current instant.
     *
     * @param secretKey The Base32-encoded secret key (spaces are auto-removed).
     * @return A 6-digit zero-padded TOTP code.
     */
    public static String getTOTPCode(String secretKey) {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Secret key cannot be null or empty");
        }

        // Remove spaces which are common in manual copy-paste
        String cleanedSecret = secretKey.replace(" ", "");
        int code = gAuth.getTotpPassword(cleanedSecret);

        // Format as a 6-digit zero-padded string (e.g. 054123)
        return String.format("%06d", code);
    }
}
