package com.orderflux.backend.dto.response;

import com.orderflux.backend.model.User;
import com.orderflux.backend.model.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;

    /**
     * phoneNumber included in response:
     *   User needs to see what phone we have on file.
     *   Safe to expose — it's their own data.
     */
    private String phoneNumber;

    private Role role;
    private Boolean isEnabled;

    /**
     * isEmailVerified included:
     *   Frontend uses this to decide whether to show
     *   "Please verify your email" banner.
     *   Critical for UX after registration.
     */
    private Boolean isEmailVerified;

    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .isEnabled(user.getIsEnabled())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}