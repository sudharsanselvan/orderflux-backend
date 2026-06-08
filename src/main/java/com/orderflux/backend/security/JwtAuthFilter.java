package com.orderflux.backend.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JwtAuthFilter — Intercepts every HTTP request and validates JWT.
 *
 * OncePerRequestFilter:
 *   Guarantees this filter runs exactly ONCE per request.
 *   Spring's filter chain can sometimes call filters multiple times
 *   (e.g., during error forwarding). This base class prevents that.
 *
 * Filter execution order for every request:
 *   Request → JwtAuthFilter → SecurityFilterChain → Controller
 *
 * What this filter does:
 *   1. Check if Authorization header exists with Bearer token
 *   2. Extract email from token
 *   3. Load user from DB
 *   4. Validate token
 *   5. Set authentication in SecurityContext
 *   6. Pass request to next filter
 *
 * If no token: just pass through (Spring Security will handle
 * the case where protected endpoints require authentication)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter{
	private final JwtService jwtService;
	private final CustomUserDetailsService userDetailsSerivce;
	
	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		
		// Step 1: Get Authorization header
		final String authHeader = request.getHeader("Authorization");
		
		//If no header or doesn't start with "Bearer " → skip JWT processing
        // Request continues unauthenticated (public endpoints still work)
		if(authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		
		// Step 2: Extract token (remove "Bearer " prefix, 7 characters)
		final String jwt = authHeader.substring(7);
		
		try {
			// Step 3: Extract email from token
			final String email = jwtService.extractEmail(jwt);
			
			/**
             * SecurityContextHolder.getContext().getAuthentication()
             *
             * SecurityContext = Spring's thread-local storage for
             *   the current request's authentication state.
             *
             * If authentication is already set → skip (token already processed).
             * If null → process the token.
             */
			if(email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				
				// step 4: load user from DB
				UserDetails userDetails = userDetailsSerivce.loadUserByUsername(email);
				
				// step 5: validate token against loader user
				if(jwtService.isTokenValid(jwt, userDetails)) {
					/**
                     * UsernamePasswordAuthenticationToken:
                     *   Spring Security's standard authentication object.
                     *   Parameters:
                     *     1. principal   → UserDetails (who is authenticated)
                     *     2. credentials → null (password not needed post-auth)
                     *     3. authorities → roles/permissions
                     */
					
					UsernamePasswordAuthenticationToken authToken = 
							new UsernamePasswordAuthenticationToken(
									userDetails,
									null,
									userDetails.getAuthorities()
									);
					//Attach request details (Ip address, session Id)
					authToken.setDetails(
							new WebAuthenticationDetailsSource()
							.buildDetails(request)
							);
					
					// Step 6: Set authentication in Security Context
                    // From this point, Spring Security knows who the user is
					
					SecurityContextHolder.getContext()
					.setAuthentication(authToken);
					
					log.debug("Authenticated user: {}",email);
				}
			}
		}catch(Exception e) {
			// Token is invalid/expired/tampered — don't set authentication
            // Request continues unauthenticated
			log.warn("JWT validation failed: {}",e.getMessage());
		}
		// Step 7: Always continue the filter chain
		filterChain.doFilter(request, response);
	}
}
