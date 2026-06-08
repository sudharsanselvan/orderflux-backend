package com.orderflux.backend.service;

import com.orderflux.backend.dto.request.RegisterRequest;
import com.orderflux.backend.dto.response.OtpResponse;
import com.orderflux.backend.dto.response.UserResponse;
import com.orderflux.backend.exception.BadRequestException;
import com.orderflux.backend.exception.DuplicateResourceException;
import com.orderflux.backend.exception.ResourceNotFoundException;
import com.orderflux.backend.model.User;
import com.orderflux.backend.model.enums.Role;
import com.orderflux.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    /**
     * Register new user — updated flow:
     *
     * OLD flow: save user → return UserResponse (active immediately)
     *
     * NEW flow:
     *   1. Validate email uniqueness
     *   2. Validate phone uniqueness       ← NEW
     *   3. Build user with isEmailVerified=false ← NEW
     *   4. Save user
     *   5. Generate + send OTP             ← NEW
     *   6. Return OtpResponse (not UserResponse) ← CHANGED
     *
     * Return type changed from UserResponse to OtpResponse:
     *   We no longer return user data on registration.
     *   We return OTP confirmation — "check your email".
     *   User data is only returned after successful login.
     */
    public OtpResponse registerUser(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Step 1: Email uniqueness check
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                "User", "email", request.getEmail()
            );
        }

        // Step 2: Phone uniqueness check ← NEW
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicateResourceException(
                "User", "phone number", request.getPhoneNumber()
            );
        }

        // Step 3: Build user
        // isEmailVerified defaults to false via @Builder.Default
        // isEnabled defaults to true — account exists but unverified
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(Role.Role_Customer)
                .build();
        // isEmailVerified = false automatically from @Builder.Default
        // isEnabled = true automatically from @Builder.Default
        // isLocked = false automatically from @Builder.Default

        // Step 4: Save user — gets ID assigned
        User savedUser = userRepository.save(user);
        log.info("User saved with id: {}, awaiting verification",
                savedUser.getId());

        // Step 5: Generate OTP + send email
        // OtpService handles: invalidate old → generate → save → email
        OtpResponse otpResponse = otpService.generateAndSendOtp(savedUser);

        log.info("Registration complete, OTP sent to: {}", request.getEmail());

        return otpResponse;
    }

    /**
     * Get all users — unchanged
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");
        List<User> users = userRepository.findAll();
        log.info("Total users found: {}", users.size());
        return users.stream()
                .map(UserResponse::from)
                .toList();
    }

    /**
     * Get user by ID — unchanged
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.info("Fetching user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException("User", "id", id)
                );
        return UserResponse.from(user);
    }
}