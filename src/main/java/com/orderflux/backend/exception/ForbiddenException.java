package com.orderflux.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticated user attempts an action they don't have
 * permission for.
 * Maps to HTTP 403 Forbidden.
 *
 * Usage:
 *   throw new ForbiddenException("You can only edit your own products");
 */

@SuppressWarnings("serial")
public class ForbiddenException extends BaseException {
	
	public ForbiddenException(String message) {
		super(message,HttpStatus.FORBIDDEN);
	}
	
}
