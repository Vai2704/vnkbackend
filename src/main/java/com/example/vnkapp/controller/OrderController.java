package com.example.vnkapp.controller;

import com.example.vnkapp.dto.common.ApiResponseDto;
import com.example.vnkapp.dto.common.PagedResponseDto;
import com.example.vnkapp.dto.order.CancelOrderRequestDto;
import com.example.vnkapp.dto.order.OrderResponseDto;
import com.example.vnkapp.dto.order.OrderSummaryResponseDto;
import com.example.vnkapp.dto.order.PlaceOrderRequestDto;
import com.example.vnkapp.dto.user.UserResponseDto;
import com.example.vnkapp.enums.order.OrderStatus;
import com.example.vnkapp.security.AuthenticatedUser;
import com.example.vnkapp.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody PlaceOrderRequestDto request) {
        log.info("Place order for user: {}, addressId: {}", currentUser.getId(), request.addressId());
        try {
            OrderResponseDto order = orderService.placeOrder(currentUser.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponseDto<>("Ok", null, order));
        } catch (IllegalArgumentException ex) {
            log.warn("Place order failed for user: {} - {}", currentUser.getId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Place order error for user: {}", currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't place order due to some issue."));
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserOrders(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Get orders for user: {}, status: {}, page: {}", currentUser.getId(), status, page);
        try {
            Page<OrderSummaryResponseDto> orders = orderService.getUserOrders(currentUser.getId(), status, page, size);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, PagedResponseDto.from(orders)));
        } catch (Exception ex) {
            log.error("Get orders error for user: {}", currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch orders due to some issue."));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderDetails(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id) {
        log.info("Get order {} for user: {}", id, currentUser.getId());
        try {
            OrderResponseDto order = orderService.getOrderDetails(currentUser.getId(), id);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, order));
        } catch (IllegalArgumentException ex) {
            log.warn("Order {} not found for user: {}", id, currentUser.getId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Get order {} error for user: {}", id, currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch order details due to some issue."));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody CancelOrderRequestDto request) {
        log.info("Cancel order {} for user: {}", id, currentUser.getId());
        try {
            OrderResponseDto order = orderService.cancelOrder(currentUser.getId(), id, request);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, order));
        } catch (IllegalArgumentException ex) {
            log.warn("Cancel order {} failed for user: {} - {}", id, currentUser.getId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Cancel order {} error for user: {}", id, currentUser.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't cancel order due to some issue."));
        }
    }
}
