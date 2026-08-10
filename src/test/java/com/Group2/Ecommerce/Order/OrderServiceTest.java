package com.Group2.Ecommerce.Order;

import com.Group2.Ecommerce.Common.Exception.OutOfStockException;
import com.Group2.Ecommerce.Common.Exception.ResourceNotFoundException;
import com.Group2.Ecommerce.Order.Dto.OrderItemRequest;
import com.Group2.Ecommerce.Order.Dto.OrderRequest;
import com.Group2.Ecommerce.Order.Dto.OrderResponse;
import com.Group2.Ecommerce.Product.Product;
import com.Group2.Ecommerce.Product.ProductService;
import com.Group2.Ecommerce.User.Address;
import com.Group2.Ecommerce.User.AddressRepository;
import com.Group2.Ecommerce.User.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Address address;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setRole(com.Group2.Ecommerce.User.Role.CUSTOMER);

        address = new Address();
        address.setId(1L);
        address.setUser(user);
        address.setFullName("Test User");
        address.setLine1("123 Main Street");
        address.setCity("Lagos");
        address.setPostalCode("100001");
        address.setCountry("NG");

        product1 = new Product();
        product1.setId(1L);
        product1.setName("Wireless Headphones");
        product1.setPrice(new BigDecimal("89.99"));

        product2 = new Product();
        product2.setId(2L);
        product2.setName("Phone Case");
        product2.setPrice(new BigDecimal("15.00"));

        // Simulates an authenticated request — OrderService reads the
        // current user from here, exactly as it does in production via
        // the JWT filter setting the security context.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createOrder_computesTotalCorrectly_acrossMultipleItems() {
        OrderRequest request = new OrderRequest();
        request.setAddressId(1L);
        request.setItems(List.of(
                itemRequest(1L, 2), // 89.99 * 2 = 179.98
                itemRequest(2L, 3)  // 15.00 * 3 = 45.00
        ));                        // total = 224.98

        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        when(productService.findEntityById(1L)).thenReturn(product1);
        when(productService.findEntityById(2L)).thenReturn(product2);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.getTotalAmount()).isEqualByComparingTo("224.98");
        assertThat(response.getItems()).hasSize(2);
    }

    @Test
    void createOrder_decrementsStock_forEveryItem() {
        OrderRequest request = new OrderRequest();
        request.setAddressId(1L);
        request.setItems(List.of(itemRequest(1L, 2), itemRequest(2L, 3)));

        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        when(productService.findEntityById(1L)).thenReturn(product1);
        when(productService.findEntityById(2L)).thenReturn(product2);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.createOrder(request);

        // Confirms every line item actually goes through the atomic
        // decrement check — this is what prevents overselling.
        verify(productService).decrementStock(1L, 2);
        verify(productService).decrementStock(2L, 3);
    }

    @Test
    void createOrder_throwsOutOfStock_andNeverSaves_whenAnyItemInsufficient() {
        OrderRequest request = new OrderRequest();
        request.setAddressId(1L);
        request.setItems(List.of(itemRequest(1L, 2), itemRequest(2L, 3)));

        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        // First item's stock check passes, second's fails —
        // simulates a genuine out-of-stock mid-order.
        when(productService.findEntityById(1L)).thenReturn(product1);
        doNothing().when(productService).decrementStock(1L, 2);
        doThrow(new OutOfStockException("Insufficient stock for product: 2"))
                .when(productService).decrementStock(2L, 3);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(OutOfStockException.class);

        // The whole method is @Transactional, so even though product1's
        // stock was decremented before the failure, nothing should be
        // persisted — the transaction rolls back everything.
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_throwsResourceNotFound_whenAddressDoesNotBelongToUser() {
        User otherUser = new User();
        otherUser.setId(2L);
        Address someoneElsesAddress = new Address();
        someoneElsesAddress.setId(5L);
        someoneElsesAddress.setUser(otherUser);

        OrderRequest request = new OrderRequest();
        request.setAddressId(5L);
        request.setItems(List.of(itemRequest(1L, 1)));

        when(addressRepository.findById(5L)).thenReturn(Optional.of(someoneElsesAddress));

        // Same 404 behavior as browsing someone else's address directly —
        // don't reveal that address ID 5 exists at all.
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productService, never()).decrementStock(any(), anyInt());
    }

    @Test
    void createOrder_throwsResourceNotFound_whenAddressDoesNotExist() {
        OrderRequest request = new OrderRequest();
        request.setAddressId(99L);
        request.setItems(List.of(itemRequest(1L, 1)));

        when(addressRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_throwsResourceNotFound_whenOrderDoesNotExist() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStatus_updatesOrder_whenExists() {
        Order order = new Order();
        order.setId(1L);
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.TEN);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.updateStatus(1L, OrderStatus.SHIPPED);

        assertThat(response.getStatus()).isEqualTo("SHIPPED");
    }

    @Test
    void getAllOrders_returnsAllOrders_paginated() {
        Order order1 = new Order();
        order1.setId(1L);
        order1.setUser(user);
        order1.setStatus(OrderStatus.PENDING);
        order1.setTotalAmount(new BigDecimal("10.00"));

        Order order2 = new Order();
        order2.setId(2L);
        order2.setUser(user);
        order2.setStatus(OrderStatus.PAID);
        order2.setTotalAmount(new BigDecimal("20.00"));

        PageRequest pageable = PageRequest.of(0, 10);
        when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(order1, order2)));

        Page<OrderResponse> result = orderService.getAllOrders(pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("PENDING");
        assertThat(result.getContent().get(1).getStatus()).isEqualTo("PAID");
    }

    private OrderItemRequest itemRequest(Long productId, int quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }
}