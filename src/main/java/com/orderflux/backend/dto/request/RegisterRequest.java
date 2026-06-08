package com.orderflux.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must contain uppercase, lowercase, number and special character"
    )
    private String password;

    /**
     * Phone number validation:
     *
     * @NotBlank: required — can't register without phone
     *
     * @Pattern: validates international format
     *   +91-9876543210  ✅
     *   9876543210      ✅
     *   +1-800-555-0199 ✅
     *   abc123          ❌
     *
     * Why validate here (DTO) not in entity?
     *   Entity = database concern (column type, nullable)
     *   DTO    = API contract concern (what user sends us)
     *   Validation belongs at the boundary where data enters
     */
    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^[+]?[0-9]{10,15}$",
        message = "Phone number must be 10-15 digits, optionally starting with +"
    )
    private String phoneNumber;
}