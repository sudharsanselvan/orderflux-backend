package com.orderflux.backend.exception;

import com.orderflux.backend.util.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler — Centralized exception handling for all controllers.
 *
 * @RestControllerAdvice:
 *   A specialization of @ControllerAdvice that adds @ResponseBody.
 *   Intercepts exceptions thrown from ANY @RestController in the app.
 *   Instead of letting Spring return its ugly default error format,
 *   we catch exceptions here and shape the response ourselves.
 *
 *   Think of it as a "catch block for the entire HTTP layer."
 *
 * @Slf4j (Lombok):
 *   Generates: private static final Logger log = LoggerFactory.getLogger(...)
 *   Gives us log.info(), log.warn(), log.error() without boilerplate.
 *
 * How it works:
 *   1. Controller method throws an exception
 *   2. Spring scans @RestControllerAdvice beans for a matching @ExceptionHandler
 *   3. Matching is done by exception type (most specific match wins)
 *   4. The handler method runs and returns the shaped error response
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles all our custom business exceptions in one place.
     *
     * Because all custom exceptions extend BaseException which carries
     * HttpStatus, we handle them in ONE method instead of writing a
     * separate handler for each exception type.
     *
     * @ExceptionHandler(BaseException.class):
     *   Catches BaseException AND any subclass of it.
     *   ResourceNotFoundException, DuplicateResourceException,
     *   BadRequestException, ForbiddenException — all caught here.
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex) {

        // Log as WARN — these are expected business errors, not bugs
        log.warn("Business exception: [{}] {}", ex.getStatus(), ex.getMessage());

        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles @Valid / @Validated failures from request body validation.
     *
     * When a @RequestBody fails validation (e.g., email is blank, price
     * is negative), Spring throws MethodArgumentNotValidException.
     * It contains ALL validation errors, not just the first one.
     *
     * We extract each field's error message and return them as a map:
     * {
     *   "email": "Please provide a valid email address",
     *   "password": "Password must contain uppercase, lowercase..."
     * }
     *
     * Why return Map<String, String> as data?
     *   Frontend can highlight each specific field that failed.
     *   Much better UX than a generic "validation failed" message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        // getBindingResult() → contains all validation failures
        // getFieldErrors()   → list of per-field errors
        ex.getBindingResult()
          .getFieldErrors()
          .forEach((FieldError error) ->
              // fieldName → "email", "password", "firstName"
              // defaultMessage → the message you wrote in @NotBlank(message=...)
              errors.put(error.getField(), error.getDefaultMessage())
          );

        log.warn("Validation failed: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }

    /**
     * Catch-all handler — last line of defense.
     *
     * ANY exception not caught by the handlers above lands here.
     * We:
     *   1. Log it as ERROR (this is a real bug, not expected behavior)
     *   2. Return a generic message — NEVER expose internal details
     *
     * Why not return ex.getMessage() here?
     *   Unhandled exceptions might contain internal info:
     *   "Connection refused to jdbc:mysql://localhost:3306/..."
     *   That reveals your DB host and port. Security risk.
     *   Return a generic message, log the real one.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {

        // Log as ERROR with full stack trace — this needs to be investigated
        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                    "An unexpected error occurred. Please try again later."
                ));
    }
    
    /**
     * Handles requests to URLs that don't map to any controller.
     *
     * Why does this happen?
     *   Spring first checks all @RequestMapping routes.
     *   If none match, it tries to serve it as a static resource.
     *   If no static resource exists either → NoResourceFoundException.
     *
     * Without this handler, it falls into our generic Exception handler
     * and incorrectly returns 500. A wrong URL is the CLIENT'S fault → 404.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(
            NoResourceFoundException ex) {

        log.warn("No handler found for request: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                    "The requested endpoint does not exist"
                ));
    }
    
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex) {

        log.warn("Method not allowed: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(
                    "HTTP method not supported for this endpoint"
                ));
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException ex) {

        log.warn("Failed login attempt: bad credentials");

        // IMPORTANT: Never say "password is wrong" or "email not found"
        // Always use a generic message — don't help attackers know
        // which part of credentials is wrong
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid email or password"));
    }
}