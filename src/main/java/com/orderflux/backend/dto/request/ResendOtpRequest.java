package com.orderflux.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ResendOtpRequest — Sent when user didn't receive OTP
 * or their OTP expired.
 *
 * Only needs email — we look up the user and generate fresh OTP.
 *
 * Security note:
 *   We always return the same response whether email exists or not.
 *   "If this email is registered, a new OTP has been sent."
 *   → Prevents email enumeration attacks.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ResendOtpRequest {
	
	@NotBlank(message="Email is required")
	@Email(message="Please provide a valid email address")
	private String email;
}
