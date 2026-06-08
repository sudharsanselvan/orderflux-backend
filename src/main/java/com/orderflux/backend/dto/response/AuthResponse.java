package com.orderflux.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * AuthResponse — Returned after successful login or register.
 *
 * Contains:
 *   accessToken  → JWT the client sends in every request
 *   tokenType    → Always "Bearer" — part of the OAuth2 standard
 *   user         → Basic user info so client doesn't need a second request
 */

@Getter
@Builder
public class AuthResponse {
	
	private String accessToken;
	private String tokenType;
	private UserResponse user;
	
	public static AuthResponse of(String token, UserResponse user)
	{
		return AuthResponse.builder()
				.accessToken(token)
				.tokenType("Bearer")
				.user(user)
				.build();
	}
}
