package com.orderflux.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderflux.backend.dto.request.LoginRequest;
import com.orderflux.backend.dto.request.OtpVerificationRequest;
import com.orderflux.backend.dto.request.RegisterRequest;
import com.orderflux.backend.dto.request.ResendOtpRequest;
import com.orderflux.backend.dto.response.AuthResponse;
import com.orderflux.backend.dto.response.OtpResponse;
import com.orderflux.backend.dto.response.UserResponse;
import com.orderflux.backend.service.AuthService;
import com.orderflux.backend.service.UserService;
import com.orderflux.backend.util.ApiResponse;
import com.orderflux.backend.service.OtpService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/auth")

//  --->  Clean — Lombok generates the constructor at compile time
@RequiredArgsConstructor   //  --->  generates constructor for all final fields
@Tag(name="Authentication",description = "Register, verify otp, login and User management")

public class AuthController {

	private final OtpService otpService;
    private final UserService userService;      // ← private, not public
    private final AuthService authService;
    
    /**
     * POST /api/auth/register
     *
     * Response changed:
     *   OLD: 201 + UserResponse (user data)
     *   NEW: 201 + OtpResponse  (masked email + expiry info)
     *
     * User is NOT active yet — isEmailVerified = false.
     * They must call /verify-otp before they can login.
     */
    @Operation(summary = "Register a new user",
               description = "Creates account and sends OTP to email for verification.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                     description = "Registration successful, OTP sent"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                     description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                     description = "Email or phone number already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<OtpResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        OtpResponse response = userService.registerUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                    "Registration successful. Please check your email for OTP.",
                    response
                ));
    }

    /**
     * POST /api/auth/verify-otp
     */
    @Operation(summary = "Verify email OTP",
               description = "Submit the 6-digit OTP received on email.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email verified successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(
            @Valid @RequestBody OtpVerificationRequest request) {

        otpService.verifyOtp(request.getEmail(), request.getOtpCode());

        return ResponseEntity.ok(
            ApiResponse.success("Email verified successfully. You can now login.")
        );
    }

    /**
     * POST /api/auth/resend-otp
     */
    @Operation(summary = "Resend OTP",
               description = "Request a new OTP if previous expired or not received.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "New OTP sent"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Email already verified"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<OtpResponse>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {

		OtpResponse response = otpService.resendOtp(request.getEmail());

        return ResponseEntity.ok(
            ApiResponse.success("New OTP sent to your email.", response)
        );
    }
    
    @Operation(summary="Get user by ID")
    @SecurityRequirement(name="bearerAuth")
    @GetMapping("/users/{id}")                  
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable Long id) {

        UserResponse response = userService.getUserById(id);

        return ResponseEntity.ok(
                ApiResponse.success("User fetched successfully", response));
    }
    
    @Operation(summary = "Get all users", description = "Returns all registered users.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users fetched successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied — token required")
    })
    
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(){
    	
    	List<UserResponse> users = userService.getAllUsers();
    	
    	return ResponseEntity.ok(
    			ApiResponse.success("Users fetched successfully",users)
    			);
    }
    /**
     * POST /api/auth/login
     * Public endpoint — no token required
     * Returns JWT token on successful authentication
     */
    
    @Operation(
            summary = "Login",
            description = "Authenticate with email and password. Returns JWT token."
        )
        @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials")
        })
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
    		@Valid @RequestBody LoginRequest request)
    {
    	AuthResponse response = authService.login(request);
    	
    	return ResponseEntity.ok(
    			ApiResponse.success("Login successfully",response)
    			);
    }
}