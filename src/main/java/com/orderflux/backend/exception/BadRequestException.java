package com.orderflux.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the request is semantically wrong but not a validation error.
 * Maps to HTTP 400 Bad Request.
 *
 * Usage:
 *   throw new BadRequestException("Cannot place order with empty cart");
 */

@SuppressWarnings("serial")
public class BadRequestException extends BaseException {
	
	public BadRequestException(String message) {
		super(message,HttpStatus.BAD_REQUEST);
	}
	
}
