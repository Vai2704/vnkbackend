package com.example.vnkapp.service;

import com.example.vnkapp.dto.order.CancelOrderRequestDto;
import com.example.vnkapp.dto.order.OrderItemResponseDto;
import com.example.vnkapp.dto.order.OrderResponseDto;
import com.example.vnkapp.dto.order.OrderSummaryResponseDto;
import com.example.vnkapp.dto.order.PlaceOrderRequestDto;
import com.example.vnkapp.entity.Address;
import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.CartItem;
import com.example.vnkapp.entity.Order;
import com.example.vnkapp.entity.OrderItem;
import com.example.vnkapp.entity.Product;
import com.example.vnkapp.entity.ProductImage;
import com.example.vnkapp.entity.User;
import com.example.vnkapp.enums.order.OrderStatus;
import com.example.vnkapp.repository.AddressRepository;
import com.example.vnkapp.repository.CartItemRepository;
import com.example.vnkapp.repository.CartRepository;
import com.example.vnkapp.repository.OrderItemRepository;
import com.example.vnkapp.repository.OrderRepository;
import com.example.vnkapp.repository.ProductImageRepository;
import com.example.vnkapp.repository.ProductRepository;
import com.example.vnkapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final Optional<EmailService> emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        AddressRepository addressRepository,
                        ProductRepository productRepository,
                        ProductImageRepository productImageRepository,
                        UserRepository userRepository,
                        Optional<EmailService> emailService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Transactional
    public OrderResponseDto placeOrder(UUID userId, PlaceOrderRequestDto dto) {
        log.info("Placing order for user: {}, addressId: {}", userId, dto.addressId());

        // 1. Validate address belongs to user
        Address address = addressRepository.findByIdAndUserIdActive(dto.addressId(), userId)
                .orElseThrow(() -> {
                    log.warn("Address {} not found for user: {}", dto.addressId(), userId);
                    return new IllegalArgumentException("Address not found");
                });

        // 2. Get user's cart
        var cart = cartRepository.findByUserIdActive(userId)
                .orElseThrow(() -> {
                    log.warn("Cart is empty for user: {}", userId);
                    return new IllegalArgumentException("Cart is empty");
                });

        // 3. Get cart items
        List<CartItem> cartItems = cartItemRepository.findByCartIdActive(cart.getId());
        if (cartItems.isEmpty()) {
            log.warn("Cart has no items for user: {}", userId);
            throw new IllegalArgumentException("Cart is empty");
        }
        log.debug("Processing {} cart items for user: {}", cartItems.size(), userId);

        // 4. Fetch all products and their primary images
        List<UUID> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .toList();

        Map<UUID, Product> productMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // Fetch primary images for all products
        Map<UUID, String> productImageMap = productImageRepository.findPrimaryByProductIds(productIds)
                .stream()
                .collect(Collectors.toMap(ProductImage::getProductId, ProductImage::getImageUrl));

        // 5. Validate all products exist and have sufficient stock
        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProductId());
            if (product == null || product.getStatus().equals(BaseEntity.STATUS_INACTIVE)) {
                log.warn("Product not available: {}", cartItem.getProductId());
                throw new IllegalArgumentException("Product not available: " + cartItem.getProductId());
            }
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                log.warn("Insufficient stock for product: {}, available: {}, requested: {}",
                        product.getName(), product.getStockQuantity(), cartItem.getQuantity());
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
            }
        }

        // 6. Calculate totals
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProductId());
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(itemTotal);
        }

        // TODO: Apply coupon discount if couponId is provided
        BigDecimal discountAmount = BigDecimal.ZERO;

        // TODO: Calculate shipping based on address/cart weight
        BigDecimal shippingAmount = BigDecimal.ZERO;

        // TODO: Calculate tax
        BigDecimal taxAmount = BigDecimal.ZERO;

        BigDecimal totalAmount = subtotal
                .subtract(discountAmount)
                .add(shippingAmount)
                .add(taxAmount);

        // 7. Generate order number
        String orderNumber = generateOrderNumber();

        // 8. Create order
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(userId)
                .addressId(dto.addressId())
                .couponId(dto.couponId())
                .orderStatus(OrderStatus.PENDING)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .shippingAmount(shippingAmount)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .shippingFullName(address.getFullName())
                .shippingPhone(address.getPhone())
                .shippingAddress(address.getAddressLine1() +
                        (address.getAddressLine2() != null ? ", " + address.getAddressLine2() : ""))
                .shippingCity(address.getCity())
                .shippingState(address.getState())
                .shippingPostalCode(address.getPostalCode())
                .shippingCountry(address.getCountry())
                .notes(dto.notes())
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order created: {}, orderNumber: {}, total: {}", savedOrder.getId(), orderNumber, totalAmount);

        // 9. Create order items and update product stock
        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProductId());
            String imageUrl = productImageMap.get(product.getId());

            OrderItem orderItem = OrderItem.builder()
                    .orderId(savedOrder.getId())
                    .productId(product.getId())
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .productImageUrl(imageUrl)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(product.getPrice())
                    .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                    .build();

            orderItemRepository.save(orderItem);

            // Update stock
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
            log.debug("Stock updated for product: {}, remaining: {}", product.getId(), product.getStockQuantity());
        }

        // 10. Clear cart (soft delete cart items)
        for (CartItem cartItem : cartItems) {
            cartItem.setStatus(BaseEntity.STATUS_INACTIVE);
            cartItemRepository.save(cartItem);
        }
        log.debug("Cart cleared for user: {}", userId);

        // 11. Send order confirmation email (if email service is configured)
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            String shippingAddressFormatted = String.format("%s\n%s\n%s, %s %s\n%s",
                    savedOrder.getShippingFullName(),
                    savedOrder.getShippingAddress(),
                    savedOrder.getShippingCity(),
                    savedOrder.getShippingState(),
                    savedOrder.getShippingPostalCode(),
                    savedOrder.getShippingCountry());

            emailService.ifPresent(service -> service.sendOrderConfirmation(
                    user.getEmail(),
                    user.getUsername(),
                    orderNumber,
                    totalAmount.toString(),
                    shippingAddressFormatted
            ));
        }

        // 12. Return order response
        return getOrderDetails(userId, savedOrder.getId());
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponseDto> getUserOrders(UUID userId, OrderStatus orderStatus,
                                                        int page, int size) {
        log.debug("Fetching orders for user: {}, status: {}, page: {}", userId, orderStatus, page);
        Pageable pageable = PageRequest.of(page, size);

        Page<Order> orders;
        if (orderStatus != null) {
            orders = orderRepository.findByUserIdAndOrderStatusActivePaginated(userId, orderStatus, pageable);
        } else {
            orders = orderRepository.findByUserIdActivePaginated(userId, pageable);
        }

        return orders.map(order -> {
            List<OrderItem> items = orderItemRepository.findByOrderIdActive(order.getId());
            int itemCount = items.stream().mapToInt(OrderItem::getQuantity).sum();
            return OrderSummaryResponseDto.fromEntity(order, itemCount);
        });
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrderDetails(UUID userId, UUID orderId) {
        log.debug("Fetching order {} for user: {}", orderId, userId);
        Order order = orderRepository.findByIdAndUserIdActive(orderId, userId)
                .orElseThrow(() -> {
                    log.warn("Order {} not found for user: {}", orderId, userId);
                    return new IllegalArgumentException("Order not found");
                });

        List<OrderItem> orderItems = orderItemRepository.findByOrderIdActive(order.getId());

        List<OrderItemResponseDto> items = orderItems.stream()
                .map(OrderItemResponseDto::fromEntity)
                .toList();

        return OrderResponseDto.fromEntity(order, items);
    }

    @Transactional
    public OrderResponseDto cancelOrder(UUID userId, UUID orderId, CancelOrderRequestDto dto) {
        log.info("Cancelling order {} for user: {}", orderId, userId);
        Order order = orderRepository.findByIdAndUserIdActive(orderId, userId)
                .orElseThrow(() -> {
                    log.warn("Order {} not found for user: {}", orderId, userId);
                    return new IllegalArgumentException("Order not found");
                });

        // Only PENDING or CONFIRMED orders can be cancelled
        if (order.getOrderStatus() != OrderStatus.PENDING &&
            order.getOrderStatus() != OrderStatus.CONFIRMED) {
            log.warn("Cannot cancel order {} - current status: {}", orderId, order.getOrderStatus());
            throw new IllegalArgumentException(
                    "Order cannot be cancelled. Current status: " + order.getOrderStatus());
        }

        // Update order status
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(dto.reason());
        order.setCancelledAt(Instant.now());
        orderRepository.save(order);
        log.info("Order {} cancelled, reason: {}", orderId, dto.reason());

        // Restore product stock
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdActive(order.getId());
        for (OrderItem orderItem : orderItems) {
            Product product = productRepository.findById(orderItem.getProductId()).orElse(null);
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + orderItem.getQuantity());
                productRepository.save(product);
                log.debug("Stock restored for product: {}, quantity: {}", product.getId(), orderItem.getQuantity());
            }
        }

        return getOrderDetails(userId, orderId);
    }

    private String generateOrderNumber() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = String.format("%06d", secureRandom.nextInt(1000000));
        String orderNumber = "ORD-" + datePrefix + "-" + randomSuffix;

        // Ensure uniqueness
        while (orderRepository.existsByOrderNumber(orderNumber)) {
            randomSuffix = String.format("%06d", secureRandom.nextInt(1000000));
            orderNumber = "ORD-" + datePrefix + "-" + randomSuffix;
        }

        return orderNumber;
    }
}
