package com.Group2.Ecommerce.Payment;

import com.Group2.Ecommerce.Order.Order;
import com.Group2.Ecommerce.Order.OrderEmailService;
import com.Group2.Ecommerce.Order.OrderRepository;
import com.Group2.Ecommerce.Order.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ProcessedWebhookEventRepository processedWebhookEventRepository;

    @Mock
    private OrderEmailService orderEmailService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void handleWebhook_chargeSuccess_marksPaidAndSendsConfirmation() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("100.00"));

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setOrder(order);
        payment.setReference("ref123");
        payment.setStatus(PaymentStatus.PENDING);

        when(processedWebhookEventRepository.existsById("ref123")).thenReturn(false);
        when(paymentRepository.findByReference("ref123")).thenReturn(Optional.of(payment));

        paymentService.handleWebhook("{\"event\":\"charge.success\",\"data\":{\"reference\":\"ref123\"}}");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderEmailService).sendConfirmation(order);
        verify(processedWebhookEventRepository).save(any(ProcessedWebhookEvent.class));
    }

    @Test
    void handleWebhook_chargeFailed_sendsNoConfirmation() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("100.00"));

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setOrder(order);
        payment.setReference("ref123");
        payment.setStatus(PaymentStatus.PENDING);

        when(processedWebhookEventRepository.existsById("ref123")).thenReturn(false);
        when(paymentRepository.findByReference("ref123")).thenReturn(Optional.of(payment));

        paymentService.handleWebhook("{\"event\":\"charge.failed\",\"data\":{\"reference\":\"ref123\"}}");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderEmailService, never()).sendConfirmation(any(Order.class));
    }

    @Test
    void handleWebhook_duplicateEvent_isIgnored() {
        when(processedWebhookEventRepository.existsById("ref123")).thenReturn(true);

        paymentService.handleWebhook("{\"event\":\"charge.success\",\"data\":{\"reference\":\"ref123\"}}");

        verify(paymentRepository, never()).findByReference(anyString());
        verify(orderEmailService, never()).sendConfirmation(any(Order.class));
        verify(processedWebhookEventRepository, never()).save(any(ProcessedWebhookEvent.class));
    }
}
