package com.orderflux.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * OtpResponse — Returned after registration and resend-otp.
 *
 * Why not just use ApiResponse<String>?
 *   Having a dedicated DTO lets us add fields later:
 *   - expiresInSeconds (countdown timer for frontend)
 *   - maskedEmail      (show "s***@gmail.com" for confirmation)
 *
 *   String responses can't be extended without breaking API contract.
 *
 * maskedEmail:
 *   "sudharsan@gmail.com" → "s*******@gmail.com"
 *   Shows user WHICH email we sent to without exposing full address.
 *   Useful when user has multiple email accounts.
 */
@Getter
@Builder
public class OtpResponse {

    private String maskedEmail;
    private String message;
    private Integer expiresInSeconds;

    public static OtpResponse of(String email, String message,
                                  int expiresInSeconds) {
        return OtpResponse.builder()
                .maskedEmail(maskEmail(email))
                .message(message)
                .expiresInSeconds(expiresInSeconds)
                .build();
    }

    /**
     * Email masking logic:
     *   "sudharsan@gmail.com"
     *   → take first char: "s"
     *   → mask middle:     "*******"
     *   → add domain:      "@gmail.com"
     *   → result:          "s*******@gmail.com"
     */
    private static String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return email; // too short to mask

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        String masked = localPart.charAt(0)
                + "*".repeat(localPart.length() - 1);

        return masked + domain;
    }
}