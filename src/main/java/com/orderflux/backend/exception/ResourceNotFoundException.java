package com.orderflux.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource does not exist in the database.
 * Maps to HTTP 404 Not Found.
 *
 * Usage:
 *   throw new ResourceNotFoundException("User", "id", 42L);
 *   → "User not found with id : 42"
 *
 *   throw new ResourceNotFoundException("Product not found");
 *   → "Product not found"
 */
@SuppressWarnings("serial")
public class ResourceNotFoundException extends BaseException{
	
	public ResourceNotFoundException(String message) {
		super(message,HttpStatus.NOT_FOUND);
	}
	
	/**
     * Convenience constructor for the common "X not found with Y : Z" pattern.
     * resourceName: "User", "Product", "Order"
     * fieldName:    "id", "email", "slug"
     * fieldValue:   the actual value that wasn't found
     */
	
	public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
		super(
				String.format("%s not found with %s : %s",
				resourceName,fieldName,fieldValue), 
				HttpStatus.NOT_FOUND
				);
	}
}
