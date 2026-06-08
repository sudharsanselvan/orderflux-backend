package com.orderflux.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * OtpVerificationRequest — Sent by user to verify their email.
 *
 * Contains:
 *   email   → identifies which user is verifying
 *   otpCode → the 6-digit code from their email
 *
 * Why email instead of userId?
 *   At verification time, user is not logged in (no JWT yet).
 *   Email is the only identifier they know about themselves.
 *   userId is an internal DB concept — never expose to API consumers.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerificationRequest {
	
	@NotBlank(message="Email is required")
	@Email(message="Please provide valid email address")
	private String email;
	
	/**
     * OTP validation:
     *   @Size(min=6, max=6): exactly 6 characters
     *   @Pattern: digits only — no letters, no spaces
     *
     *   "123456" ✅
     *   "12345"  ❌ (too short)
     *   "1234567"❌ (too long)
     *   "12345a" ❌ (contains letter)
     *   " 12345" ❌ (contains space — @NotBlank alone misses this)
     */
	
	@NotBlank(message="OTP code is required")
	@Size(min=6,max=6,message="Otp must be exactly 6 characters")
	@Pattern(regexp="\\d{6}",message="OTP must contain digits only")
	private String otpCode;
}
