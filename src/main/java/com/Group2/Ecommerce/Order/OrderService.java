package com.Group2.Ecommerce.Order;

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

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        User currentUser = getCurrentUser();

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + request.getAddressId()));

        if (!address.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Address not found: " + request.getAddressId());
        }

        List<OrderItemRequest> sortedItems = request.getItems().stream()
                .sorted(Comparator.comparing(OrderItemRequest::getProductId))
                .collect(Collectors.toList());

        Order order = new Order();
        order.setUser(currentUser);
        order.setStatus(OrderStatus.PENDING);

        // Snapshot the address at order time
        order.setShippingFullName(address.getFullName());
        order.setShippingLine1(address.getLine1());
        order.setShippingLine2(address.getLine2());
        order.setShippingCity(address.getCity());
        order.setShippingState(address.getState());
        order.setShippingPostalCode(address.getPostalCode());
        order.setShippingCountry(address.getCountry());
        order.setShippingPhone(address.getPhone());

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : sortedItems) {
            productService.decrementStock(itemRequest.getProductId(), itemRequest.getQuantity());

            Product product = productService.findEntityById(itemRequest.getProductId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(product.getPrice());

            order.getItems().add(orderItem);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }

        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);

        return OrderResponse.fromEntity(saved);
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

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        return OrderResponse.fromEntity(saved);
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}