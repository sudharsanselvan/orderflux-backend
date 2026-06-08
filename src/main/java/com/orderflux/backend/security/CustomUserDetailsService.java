package com.orderflux.backend.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderflux.backend.model.User;
import com.orderflux.backend.repository.UserRepository;

/**
 * CustomUserDetailsService — Bridge between Spring Security and our database.
 *
 * Spring Security doesn't know about our User entity.
 * It works with its own UserDetails interface.
 *
 * UserDetailsService has one method:
 *   UserDetails loadUserByUsername(String username)
 *
 * "Username" in Spring Security = whatever uniquely identifies a user.
 * In our app, that's email.
 *
 * Spring Security calls this:
 *   1. During login — to load user and verify password
 *   2. During JWT filter — to load user and set authentication context
 */
@Service
public class CustomUserDetailsService implements UserDetailsService{
	private final UserRepository userRepository;
	
	public CustomUserDetailsService (UserRepository userRepository) {
		this.userRepository=userRepository;
	}
	
	/**
     * Load user by email.
     *
     * Returns Spring Security's UserDetails — an interface with:
     *   getUsername()    → email
     *   getPassword()    → hashed password
     *   getAuthorities() → roles/permissions
     *   isEnabled()      → account active?
     *   isAccountNonLocked() → not locked?
     *
     * We use Spring's built-in User builder (org.springframework.security.core.userdetails.User)
     * to construct a UserDetails from our entity.
     * This avoids making our entity implement UserDetails (bad coupling).
     */
	@Override
	@Transactional(readOnly=true)
	public UserDetails loadUserByUsername(String email)	throws UsernameNotFoundException{
			User user = userRepository
					.findByEmail(email)
					.orElseThrow(() -> 
					new UsernameNotFoundException("User not found with email: " + email)
					);
			
			/**
	         * SimpleGrantedAuthority: Spring Security's representation of a role.
	         * "ROLE_CUSTOMER" → new SimpleGrantedAuthority("ROLE_CUSTOMER")
	         *
	         * Spring Security expects roles prefixed with "ROLE_"
	         * when using hasRole() checks.
	         * hasRole("CUSTOMER") internally checks for "ROLE_CUSTOMER".
	         */
			return org.springframework.security.core.userdetails.User
					.withUsername(user.getEmail())
					.password(user.getPassword())
					.authorities(List.of(
							new SimpleGrantedAuthority(user.getRole().name())
							))
					.accountLocked(user.getIsLocked())
					.disabled(!user.getIsEnabled())
					.build();
		}
}
