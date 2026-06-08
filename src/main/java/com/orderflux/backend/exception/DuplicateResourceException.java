package com.orderflux.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to create a resource that already exists.
 * Maps to HTTP 409 Conflict.
 *
 * Usage:
 *   throw new DuplicateResourceException("User", "email", "john@example.com");
 *   → "User already exists with email : john@example.com"
 */

@SuppressWarnings("serial")
public class DuplicateResourceException extends BaseException {
	
	public DuplicateResourceException(String message) {
		super(message,HttpStatus.CONFLICT);
	}
	
	public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
		super(
				String.format("%s already exists with %s : %s",
						resourceName,fieldName,fieldValue),HttpStatus.CONFLICT
				);
	}
	
}
