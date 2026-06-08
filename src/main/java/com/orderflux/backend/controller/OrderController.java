package com.orderflux.backend.controller;

import com.orderflux.backend.dto.request.PlaceOrderRequest;
import com.orderflux.backend.dto.response.OrderResponse;
import com.orderflux.backend.dto.response.PageResponse;
import com.orderflux.backend.service.OrderService;
import com.orderflux.backend.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * OrderController — HTTP layer for order operations.
 *
 * @AuthenticationPrincipal UserDetails:
 *   Spring Security injects the currently authenticated user.
 *   We extract the email (username) and pass to service.
 *   This is safer than accepting userId from request body
 *   (client could send someone else's userId).
 *
 * Security principle: NEVER trust client-sent user identity.
 *   Always extract identity from the JWT token via SecurityContext.
 *   @AuthenticationPrincipal does this automatically.
 */
@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders", description = "Order placement and management")
public class OrderController {

    private final OrderService orderService;

    /**
     * POST /api/orders
     * Place a new order for the authenticated user.
     */
    @Operation(summary = "Place a new order",
               description = "Creates order, validates stock, " +
                             "decrements inventory.")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PlaceOrderRequest request) {

        String email = userDetails.getUsername();
        OrderResponse response = orderService.placeOrder(email, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                    "Order placed successfully", response
                ));
    }

    /**
     * GET /api/orders
     * Get all orders for authenticated user (paginated).
     */
    @Operation(summary = "Get my orders",
               description = "Returns paginated order history " +
                             "for authenticated user.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        String email = userDetails.getUsername();
        PageResponse<OrderResponse> response =
                orderService.getMyOrders(email, page, size);

        return ResponseEntity.ok(
            ApiResponse.success("Orders fetched successfully", response)
        );
    }

    /**
     * GET /api/orders/{id}
     * Get specific order (only if it belongs to authenticated user).
     */
    @Operation(summary = "Get order by ID",
               description = "Returns order details. " +
                             "User can only access their own orders.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        String email = userDetails.getUsername();
        OrderResponse response = orderService.getOrderById(email, id);

        return ResponseEntity.ok(
            ApiResponse.success("Order fetched successfully", response)
        );
    }

    /**
     * PATCH /api/orders/{id}/cancel
     * Cancel an order (only PENDING or CONFIRMED).
     *
     * Why PATCH not DELETE?
     *   We're changing the status, not deleting the resource.
     *   The order still exists — just in CANCELLED state.
     *   PATCH = partial update. DELETE = remove resource.
     *   REST semantics matter.
     */
    @Operation(summary = "Cancel an order",
               description = "Cancels order and restores stock. " +
                             "Only PENDING or CONFIRMED orders can be cancelled.")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        String email = userDetails.getUsername();
        OrderResponse response = orderService.cancelOrder(email, id);

        return ResponseEntity.ok(
            ApiResponse.success("Order cancelled successfully", response)
        );
    }
}