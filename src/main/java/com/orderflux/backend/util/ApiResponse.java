package com.orderflux.backend.util;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard API response wrapper for ALL endpoints.
 *
 * Every response — success or error — uses this shape:
 * {
 *   "success": true,
 *   "message": "Operation successful",
 *   "data": { ... },       ← null if error
 *   "timestamp": "2024-..."
 * }
 *
 * @param <T> The type of the data payload
 */
@Getter
@Builder                        // Lombok: generates builder pattern
@AllArgsConstructor
@NoArgsConstructor(force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)  // Don't serialize null fields
public class ApiResponse <T>{

    private final boolean success;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    // Static factory methods — clean API for callers

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    public static <T> ApiResponse<T> info(String message,Object x){
    	return ApiResponse.<T>builder()
    			.success(true)
    			.message(message)
    			.timestamp(LocalDateTime.now())
    			.build();
    }
}