package com.orderflux.backend.service;

import com.orderflux.backend.dto.response.OtpResponse;
import com.orderflux.backend.exception.BadRequestException;
import com.orderflux.backend.exception.ResourceNotFoundException;
import com.orderflux.backend.model.OtpVerification;
import com.orderflux.backend.model.User;
import com.orderflux.backend.repository.OtpVerificationRepository;
import com.orderflux.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * OtpService — Handles all OTP lifecycle operations.
 *
 * Responsibilities:
 *   1. Generate secure 6-digit OTP
 *   2. Save OTP to DB with expiry time
 *   3. Verify OTP submitted by user
 *   4. Invalidate previous OTPs on resend
 *
 * Why a separate service and not inside UserService?
 *   Single Responsibility Principle.
 *   OTP logic is complex enough to deserve its own class.
 *   UserService should focus on user management, not OTP mechanics.
 *   OtpService can be reused for phone OTP, password reset OTP later.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    public static Object otpService;
	private final OtpVerificationRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${otp.expiry.minutes}")
    private int otpExpiryMinutes;

    /**
     * SecureRandom vs Random:
     *
     * Random:
     *   Uses a predictable seed (system time).
     *   An attacker who knows the seed can predict all future values.
     *   NEVER use for security-sensitive values.
     *
     * SecureRandom:
     *   Uses OS-level entropy source (hardware events, /dev/urandom).
     *   Cryptographically secure — unpredictable.
     *   Slower than Random but required for OTPs, tokens, keys.
     *
     * static final: one instance shared across all method calls.
     *   SecureRandom is thread-safe and expensive to initialize.
     *   Creating a new instance per call wastes resources.
     */
    private static final SecureRandom secureRandom = new SecureRandom();

    // ─── Public API ───────────────────────────────────────────

    /**
     * Generate OTP, save to DB, send email.
     * Called by UserService during registration.
     *
     * Flow:
     *   1. Invalidate any existing unused OTPs for this user
     *   2. Generate new 6-digit OTP
     *   3. Save to DB with expiry
     *   4. Send email asynchronously
     *   5. Return OtpResponse with masked email
     */
    @Transactional
    public OtpResponse generateAndSendOtp(User user) {
        log.info("Generating OTP for user: {}", user.getEmail());

        // Step 1: Invalidate previous OTPs — clean slate
        otpRepository.invalidateAllOtpsForUser(user);

        // Step 2: Generate secure 6-digit code
        String otpCode = generateOtpCode();

        // Step 3: Save to DB with expiry time
        saveOtp(user, otpCode);

        // Step 4: Send email — @Async, returns immediately
        emailService.sendOtpEmail(user.getEmail(), otpCode, user.getFirstName());

        log.info("OTP generated and email triggered for: {}", user.getEmail());

        // Step 5: Return response
        return OtpResponse.of(
                user.getEmail(),
                "Verification code sent to your email",
                otpExpiryMinutes * 60   // convert minutes to seconds for frontend
        );
    }

    /**
     * Verify OTP submitted by user.
     *
     * Validation chain:
     *   1. Find user by email → 404 if not found
     *   2. Check already verified → 400 if true
     *   3. Find valid (unused + unexpired) OTP → 400 if none
     *   4. Compare OTP codes → 400 if mismatch
     *   5. Mark OTP as used
     *   6. Mark user as verified
     *   7. Save user
     *
     * @param email   the user's email
     * @param otpCode the 6-digit code submitted by user
     */
    @Transactional
    public void verifyOtp(String email, String otpCode) {
        log.info("Verifying OTP for email: {}", email);

        // Step 1: Find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                    new ResourceNotFoundException("User", "email", email)
                );

        // Step 2: Already verified check
        if (user.getIsEmailVerified()) {
            throw new BadRequestException(
                "Email is already verified. Please login."
            );
        }

        // Step 3: Find valid OTP
        // findValidOtpByUser checks: isUsed=false AND expiryTime > now
        OtpVerification otp = otpRepository
                .findValidOtpByuser(user, LocalDateTime.now())
                .orElseThrow(() ->
                    new BadRequestException(
                        "OTP has expired or is invalid. " +
                        "Please request a new one."
                    )
                );

        // Step 4: Compare codes
        /**
         * Why not use == for String comparison?
         *   == compares object references, not values.
         *   "123456" == "123456" can be false for different String objects.
         *   .equals() compares actual character values — always correct.
         *
         * Production enhancement: use MessageDigest.isEqual()
         *   for constant-time comparison to prevent timing attacks.
         *   For 6-digit OTPs the risk is minimal, but good habit.
         */
        if (!otp.getOtpCode().equals(otpCode)) {
            log.warn("Invalid OTP attempt for email: {}", email);
            throw new BadRequestException(
                "Invalid OTP. Please check and try again."
            );
        }

        // Step 5: Mark OTP as used — prevents replay attacks
        otp.setIsUsed(true);
        otpRepository.save(otp);

        // Step 6 + 7: Mark user as verified and save
        user.setIsEmailVerified(true);
        userRepository.save(user);

        log.info("Email verified successfully for user: {}", email);
    }

    /**
     * Resend OTP — generates fresh OTP for unverified user.
     *
     * Security checks:
     *   1. User must exist
     *   2. User must NOT already be verified
     *      (no point resending if already verified)
     *
     * Note on email enumeration:
     *   In a high-security system, always return the same response
     *   whether user exists or not. Here we throw 404 for simplicity
     *   since this is a learning project. Production systems use:
     *   "If this email is registered, a new OTP has been sent"
     */
    @Transactional
    public OtpResponse resendOtp(String email) {
        log.info("Resend OTP requested for: {}", email);

        // Find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                    new ResourceNotFoundException("User", "email", email)
                );

        // Already verified check
        if (user.getIsEmailVerified()) {
            throw new BadRequestException(
                "Email is already verified. Please login."
            );
        }

        // Generate and send fresh OTP
        // generateAndSendOtp() invalidates old OTPs automatically
        return generateAndSendOtp(user);
    }

    // ─── Private Helpers ──────────────────────────────────────

    /**
     * Generate a cryptographically secure 6-digit OTP.
     *
     * secureRandom.nextInt(900000) → 0 to 899999
     * + 100000                     → 100000 to 999999
     * Always 6 digits, never starts with 0.
     *
     * String.format("%06d", ...) → ensures leading zeros preserved
     * Example: if somehow we got 5 digits → "012345" not "12345"
     *
     * Why not UUID or random alphanumeric?
     *   Users type OTPs manually on mobile.
     *   6 digits are easiest to read from email and type.
     *   Alphanumeric ("aB3xK9") is error-prone (0 vs O, 1 vs l).
     */
    private String generateOtpCode() {
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Persist OTP to database.
     *
     * expiryTime = now + configured minutes (default 5).
     * isUsed defaults to false via @Builder.Default in entity.
     */
    private void saveOtp(User user, String otpCode) {
    	OtpVerification otpVerification = OtpVerification.create(
                user,
                otpCode,
                LocalDateTime.now().plusMinutes(otpExpiryMinutes)
        );

        otpRepository.save(otpVerification);
        log.debug("OTP saved for user: {} expires at: {}",
                user.getEmail(),
                otpVerification.getExpiryTime());
    }
}