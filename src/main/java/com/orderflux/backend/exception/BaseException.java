package com.orderflux.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * BaseException — Parent of all OrderFlux custom exceptions.
 *
 * Why extend RuntimeException, not Exception?
 *
 * Checked Exceptions (extends Exception):
 *   Compiler FORCES you to catch or declare them (throws clause).
 *   Good for recoverable conditions you EXPECT callers to handle
 *   e.g., FileNotFoundException when reading config files
 *
 * Unchecked Exceptions (extends RuntimeException):
 *   No forced handling. They propagate up naturally.
 *   Spring's exception handling infrastructure catches them at the
 *   @ControllerAdvice level — exactly what we want.
 *   Business exceptions (not found, conflict, forbidden) are
 *   never "recoverable" mid-request — let them propagate cleanly.
 *
 * Why carry HttpStatus inside the exception?
 *   The exception KNOWS what HTTP response it should produce.
 *   Our GlobalExceptionHandler just reads it and maps it.
 *   No giant if-else chain needed in the handler.
 */

@SuppressWarnings("serial")
public class BaseException extends RuntimeException {
	
	private final HttpStatus status;
	
	public BaseException(String message, HttpStatus status) {
		super(message);
		this.status=status;
	}

	public HttpStatus getStatus() {
		return status;
	}

}
