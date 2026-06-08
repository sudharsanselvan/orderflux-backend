package com.orderflux.backend.controller;

import com.orderflux.backend.util.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * HealthController — System health check endpoints.
 *
 * @RestController = @Controller + @ResponseBody
 *   - @Controller: marks this as a Spring MVC controller (a Bean)
 *   - @ResponseBody: return values are serialized to JSON directly
 *     (instead of being treated as view names)
 *
 * @RequestMapping("/health"): all methods in this controller
 *   are prefixed with /health
 *   Full path = /api/health (because server.servlet.context-path=/api)
 */
@Slf4j
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {
	
	private final JavaMailSender mailSender;
    /**
     * GET /api/health
     *
     * @GetMapping: handles HTTP GET requests
     * ResponseEntity<?>: gives full control over HTTP response
     *   - status code
     *   - headers
     *   - body
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> checkHealth() {
        Map<String, String> healthData = Map.of(
            "status", "UP",
            "application", "OrderFlux",
            "version", "1.0.0"
        );

        return ResponseEntity.ok(
            ApiResponse.success("OrderFlux is running", healthData)
        );
    }

    /**
     * GET /api/health/ping
     * Simplest possible liveness check — used by load balancers
     */
    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<Void>> ping() {
        return ResponseEntity.ok(ApiResponse.success("pong"));
    }
    
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String,Object>>> getSystemInfo(){
    	Map<String,Object> systemInfo= new HashMap<>(); 
    	
    	systemInfo.put("javaVersion", System.getProperty("java.version"));
    	systemInfo.put("osName", System.getProperty("os.name"));
    	systemInfo.put("availableProcessors", Runtime.getRuntime().availableProcessors());
    	systemInfo.put("currentTime", LocalDateTime.now());
    	
    	return ResponseEntity.ok(
    			ApiResponse.success("System Information fetched successfully!",systemInfo)
    			);
    }
    
    @GetMapping("/test-email")
    public ResponseEntity<ApiResponse<String>> testEmail() {
        try {
            // Direct mail test — bypasses async
            jakarta.mail.internet.MimeMessage message = 
                mailSender.createMimeMessage();
            
            org.springframework.mail.javamail.MimeMessageHelper helper = 
                new org.springframework.mail.javamail.MimeMessageHelper(
                    message, false, "UTF-8"
                );
            
            helper.setTo("sudharselvan@gmail.com");  // ← your email
            helper.setSubject("OrderFlux SMTP Test");
            helper.setText("SMTP is working correctly!", false);
            
            mailSender.send(message);
            
            return ResponseEntity.ok(
                ApiResponse.success("Test email sent successfully", null)
            );
            
        } catch (Exception e) {
            return ResponseEntity.ok(
                ApiResponse.error("Email failed: " + e.getMessage())
            );
        }
    }
}