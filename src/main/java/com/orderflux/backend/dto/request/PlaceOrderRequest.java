package com.orderflux.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * PlaceOrderRequest — Customer's order submission.
 *
 * Contains:
 *   items           → list of products + quantities
 *   shippingAddress → where to deliver
 *   notes           → special instructions (optional)
 *
 * @Valid on items list:
 *   Triggers validation on each OrderItemRequest inside the list.
 *   Without @Valid, nested object validation is skipped.
 *
 * @NotEmpty vs @NotNull:
 *   @NotNull: list can't be null (but can be empty [])
 *   @NotEmpty: list can't be null AND can't be empty
 *   We use @NotEmpty — an order with zero items makes no sense.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;

    @NotBlank(message = "Shipping address is required")
    @Size(max = 500, message = "Shipping address must not exceed 500 characters")
    private String shippingAddress;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}