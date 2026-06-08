package com.orderflux.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.orderflux.backend.dto.response.UserResponse;
import com.orderflux.backend.service.UserService;
import com.orderflux.backend.util.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable Long id) {

        UserResponse user = userService.getUserById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "User fetched successfully",
                        user
                )
        );
    }
    
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(){
    	
    	List<UserResponse> users = userService.getAllUsers();
    	
    	return ResponseEntity.ok(
    			ApiResponse.success("User fetched successfully!",users)
    			);
    }
}