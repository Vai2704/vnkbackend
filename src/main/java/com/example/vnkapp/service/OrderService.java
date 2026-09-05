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
import com.example.vnkapp.entity.Payment;
import com.example.vnkapp.entity.Product;
import com.example.vnkapp.entity.ProductImage;
import com.example.vnkapp.entity.User;
import com.example.vnkapp.enums.order.OrderStatus;
import com.example.vnkapp.enums.payment.PaymentStatus;
import com.example.vnkapp.repository.AddressRepository;
import com.example.vnkapp.repository.CartItemRepository;
import com.example.vnkapp.repository.CartRepository;
import com.example.vnkapp.repository.OrderItemRepository;
import com.example.vnkapp.repository.OrderRepository;
import com.example.vnkapp.repository.PaymentRepository;
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
import java.time.Duration;
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
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final Optional<EmailService> emailService;
    private final CouponService couponService;
    private final ProductThumbnailService productThumbnailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        AddressRepository addressRepository,
                        ProductRepository productRepository,
                        ProductImageRepository productImageRepository,
                        UserRepository userRepository,
                        PaymentRepository paymentRepository,
                        PaymentService paymentService,
                        Optional<EmailService> emailService,
                        CouponService couponService,
                        ProductThumbnailService productThumbnailService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.emailService = emailService;
        this.couponService = couponService;
        this.productThumbnailService = productThumbnailService;
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
                .collect(Collectors.toMap(ProductImage::getProductId, ProductImage::getImageUrl, (a, b) -> a));

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

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (dto.couponId() != null) {
            discountAmount = couponService.calculateDiscount(dto.couponId(), userId, subtotal);
            log.info("Applied coupon {} for user {}: discount={}", dto.couponId(), userId, discountAmount);
        }

        // TODO: Calculate shipping based on address/cart weight
        BigDecimal shippingAmount = BigDecimal.ZERO;

        // TODO: Calculate tax
        BigDecimal taxAmount = BigDecimal.ZERO;

        BigDecimal totalAmount = subtotal
                .subtract(discountAmount)
                .add(shippingAmount)
                .add(taxAmount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        final BigDecimal orderTotal = totalAmount;

        // 7. Generate order number
        String orderNumber = generateOrderNumber();

        Product firstProduct = productMap.get(cartItems.get(0).getProductId());
        String orderCurrencySymbol = currencySymbolOf(firstProduct);

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
                .currencySymbol(orderCurrencySymbol)
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

        if (dto.couponId() != null) {
            couponService.markCouponUsed(dto.couponId());
        }

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
                    .currencySymbol(currencySymbolOf(product))
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

        User user = userRepository.findById(userId).orElse(null);

        // 11. Initiate payment with the gateway. Throws (rolling back the whole order) if the
        // gateway session can't be created, since an order the customer has no way to pay for
        // is worse than not creating it in the first place.
        Payment payment = paymentService.initiateNgeniusPayment(savedOrder, user);

        // 12. Send order confirmation email with the hosted payment link
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
                    orderTotal.toString(),
                    shippingAddressFormatted,
                    payment.getGatewayPaymentUrl()
            ));
        }

        // 13. Return order response
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
            String thumbnailImage = null;
            if (!items.isEmpty()) {
                OrderItem first = items.get(0);
                thumbnailImage = productThumbnailService.thumbnailOrFallback(
                        first.getProductId(), first.getProductImageUrl());
            }
            return OrderSummaryResponseDto.fromEntity(order, itemCount, thumbnailImage);
        });
    }

    /**
     * Used by GET /api/orders/{id}. For pending unpaid orders, creates a fresh N-Genius
     * hosted-page link so the customer is not sent an expired session, then returns the
     * full order including {@code paymentMessage} and {@code paymentUrl}.
     */
    @Transactional
    public OrderResponseDto getOrderDetailsRefreshingPayment(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserIdActive(orderId, userId)
                .orElseThrow(() -> {
                    log.warn("Order {} not found for user: {}", orderId, userId);
                    return new IllegalArgumentException("Order not found");
                });

        if (order.getOrderStatus() == OrderStatus.PENDING && shouldRefreshPaymentLink(order.getId())) {
            User user = userRepository.findById(userId).orElse(null);
            try {
                paymentService.retryNgeniusPayment(order, user);
                log.info("Created a new payment link for pending order {}", order.getOrderNumber());
            } catch (Exception ex) {
                log.error("Failed to create a new payment link for pending order {}",
                        order.getOrderNumber(), ex);
            }
        }

        return getOrderDetails(userId, orderId);
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
        Map<UUID, String> thumbnails = productThumbnailService.thumbnailsFor(
                orderItems.stream().map(OrderItem::getProductId).toList());

        List<OrderItemResponseDto> items = orderItems.stream()
                .map(item -> OrderItemResponseDto.fromEntity(
                        item,
                        firstNonBlank(thumbnails.get(item.getProductId()), item.getProductImageUrl())))
                .toList();

        Payment payment = paymentRepository.findByOrderIdActive(order.getId()).orElse(null);

        return OrderResponseDto.fromEntity(order, items, payment);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
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

    @Transactional
    public OrderResponseDto retryPayment(UUID userId, UUID orderId) {
        log.info("Retry payment for order {} by user: {}", orderId, userId);
        Order order = orderRepository.findByIdAndUserIdActive(orderId, userId)
                .orElseThrow(() -> {
                    log.warn("Order {} not found for user: {}", orderId, userId);
                    return new IllegalArgumentException("Order not found");
                });

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            log.warn("Cannot retry payment for order {} - current status: {}", orderId, order.getOrderStatus());
            throw new IllegalArgumentException(
                    "Payment can only be retried for pending orders. Current status: " + order.getOrderStatus());
        }

        User user = userRepository.findById(userId).orElse(null);
        paymentService.retryNgeniusPayment(order, user);

        return getOrderDetails(userId, orderId);
    }

    private static final Duration PAYMENT_LINK_REUSE_WINDOW = Duration.ofMinutes(15);

    private boolean shouldRefreshPaymentLink(UUID orderId) {
        Payment payment = paymentRepository.findByOrderIdActive(orderId).orElse(null);
        if (payment == null || payment.getGatewayPaymentUrl() == null
                || payment.getGatewayPaymentUrl().isBlank()) {
            return true;
        }
        PaymentStatus status = payment.getPaymentStatus();
        if (status == PaymentStatus.FAILED) {
            return true;
        }
        if (status != PaymentStatus.PENDING) {
            return false;
        }
        Instant createdAt = payment.getCreatedAt();
        return createdAt == null || createdAt.isBefore(Instant.now().minus(PAYMENT_LINK_REUSE_WINDOW));
    }

    private String currencySymbolOf(Product product) {
        if (product == null || product.getCurrencySymbol() == null || product.getCurrencySymbol().isBlank()) {
            return "AED";
        }
        return product.getCurrencySymbol().trim();
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
