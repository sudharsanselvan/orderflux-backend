package com.orderflux.backend.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.orderflux.backend.dto.request.LoginRequest;
import com.orderflux.backend.dto.response.AuthResponse;
import com.orderflux.backend.dto.response.UserResponse;
import com.orderflux.backend.exception.BadRequestException;
import com.orderflux.backend.exception.ResourceNotFoundException;
import com.orderflux.backend.model.User;
import com.orderflux.backend.repository.UserRepository;
import com.orderflux.backend.security.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;          // ← must exist
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
	
	/**
     * Step 1: Check email verified BEFORE authentication.
     *
     * Why check before authenticationManager.authenticate()?
     *
     * Option A: Authenticate first, then check verified
     *   Problem: If password is wrong AND unverified,
     *   attacker learns "this email exists but is unverified"
     *   by getting a different error than "invalid credentials".
     *   Information leak.
     *
     * Option B: Check verified first (our approach)
     *   Problem: reveals email exists in system.
     *   For this learning project, acceptable.
     *
     * Option C (production best practice):
     *   Always authenticate first.
     *   Only after successful auth, check verified status.
     *   Wrong password → always "Invalid credentials" (no info leak)
     *   Right password + unverified → "Please verify email"
     *
     * We'll implement Option C:
     */
	public AuthResponse login(LoginRequest request) {
	    log.info("Login attempt for email: {}", request.getEmail());

	    // Step 1: Authenticate credentials first
	    // Throws BadCredentialsException if wrong password
	    authenticationManager.authenticate(
	        new UsernamePasswordAuthenticationToken(
	            request.getEmail(),
	            request.getPassword()
	        )
	    );

	    // Step 2: Load user — credentials confirmed correct
	    User user = userRepository.findByEmail(request.getEmail())
	            .orElseThrow(() ->
	                new ResourceNotFoundException("User", "email", request.getEmail())
	            );

	    // Step 3: Check email verified AFTER confirming password correct
	    // This way: wrong password → "Invalid credentials" (not info leak)
	    // Right password + unverified → specific helpful message
	    if (!user.getIsEmailVerified()) {
	        log.warn("Login blocked — email not verified for: {}",
	                request.getEmail());
	        throw new BadRequestException(
	            "Email not verified. Please check your inbox for the OTP " +
	            "or use /auth/resend-otp to get a new one."
	        );
	    }

	    // Step 4: Check account enabled
	    if (!user.getIsEnabled()) {
	        throw new BadRequestException(
	            "Your account has been disabled. Please contact support."
	        );
	    }

	    // Step 5: Generate JWT token
	    UserDetails userDetails = userDetailsService
	            .loadUserByUsername(request.getEmail());
	    String token = jwtService.generateToken(userDetails);

	    log.info("Login successful for user id: {}", user.getId());

	    return AuthResponse.of(token, UserResponse.from(user));
	}
	
}
