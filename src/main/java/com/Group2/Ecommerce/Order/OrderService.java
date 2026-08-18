package com.Group2.Ecommerce.Order;

import com.Group2.Ecommerce.Cart.CartItem;
import com.Group2.Ecommerce.Cart.CartItemRepository;
import com.Group2.Ecommerce.Common.Exception.ResourceNotFoundException;
import com.Group2.Ecommerce.Order.Dto.OrderItemRequest;
import com.Group2.Ecommerce.Order.Dto.OrderRequest;
import com.Group2.Ecommerce.Order.Dto.OrderResponse;
import com.Group2.Ecommerce.Product.Product;
import com.Group2.Ecommerce.Product.ProductService;
import com.Group2.Ecommerce.User.Address;
import com.Group2.Ecommerce.User.AddressRepository;
import com.Group2.Ecommerce.User.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final AddressRepository addressRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderEmailService orderEmailService;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        User currentUser = getCurrentUser();
        Address address = findOwnedAddress(request.getAddressId(), currentUser);

        List<OrderItemRequest> sortedItems = request.getItems().stream()
                .sorted(Comparator.comparing(OrderItemRequest::getProductId))
                .collect(Collectors.toList());

        Order order = buildOrderShell(currentUser, address);

        for (OrderItemRequest itemRequest : sortedItems) {
            addItemToOrder(order, itemRequest.getProductId(), itemRequest.getQuantity());
        }

        return finalizeOrder(order);
    }

    // Checkout directly from the logged-in user's cart: reads every
    // CartItem, converts each into an OrderItem using the same atomic
    // stock-decrement path as manual order creation, then clears the cart
    // once the order is successfully placed.
    @Transactional
    public OrderResponse checkoutFromCart(Long addressId) {
        User currentUser = getCurrentUser();
        Address address = findOwnedAddress(addressId, currentUser);

        List<CartItem> cartItems = cartItemRepository.findByUserId(currentUser.getId());
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // Sort by product ID before locking, same deadlock-avoidance
        // reasoning as the manual order path.
        List<CartItem> sortedItems = cartItems.stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .collect(Collectors.toList());

        Order order = buildOrderShell(currentUser, address);

        for (CartItem cartItem : sortedItems) {
            addItemToOrder(order, cartItem.getProduct().getId(), cartItem.getQuantity());
        }

        OrderResponse response = finalizeOrder(order);

        cartItemRepository.deleteAll(cartItems);

        return response;
    }

    public OrderResponse getById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        return OrderResponse.fromEntity(order);
    }

    public Page<OrderResponse> getMyOrders(Pageable pageable) {
        User currentUser = getCurrentUser();
        return orderRepository.findByUserId(currentUser.getId(), pageable).map(OrderResponse::fromEntity);
    }

    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(OrderResponse::fromEntity);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        switch (newStatus) {
            case SHIPPED -> orderEmailService.sendShipped(saved);
            case DELIVERED -> orderEmailService.sendReviewRequest(saved);
            default -> {
            }
        }

        return OrderResponse.fromEntity(saved);
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        User currentUser = getCurrentUser();
        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Order not found: " + id);
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Order cannot be cancelled in " + order.getStatus() + " status");
        }

        for (var item : order.getItems()) {
            productService.restoreStock(item.getProduct().getId(), item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        return OrderResponse.fromEntity(orderRepository.save(order));
    }

    // --- shared helpers used by both createOrder and checkoutFromCart ---

    private Order buildOrderShell(User currentUser, Address address) {
        Order order = new Order();
        order.setUser(currentUser);
        order.setStatus(OrderStatus.PENDING);

        order.setShippingFullName(address.getFullName());
        order.setShippingLine1(address.getLine1());
        order.setShippingLine2(address.getLine2());
        order.setShippingCity(address.getCity());
        order.setShippingState(address.getState());
        order.setShippingPostalCode(address.getPostalCode());
        order.setShippingCountry(address.getCountry());
        order.setShippingPhone(address.getPhone());

        return order;
    }

    private void addItemToOrder(Order order, Long productId, int quantity) {
        productService.decrementStock(productId, quantity);

        Product product = productService.findEntityById(productId);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(product.getPrice());

        order.getItems().add(orderItem);
    }

    private OrderResponse finalizeOrder(Order order) {
        BigDecimal total = order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);

        return OrderResponse.fromEntity(saved);
    }

    private Address findOwnedAddress(Long addressId, User currentUser) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + addressId));

        if (!address.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Address not found: " + addressId);
        }
        return address;
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}