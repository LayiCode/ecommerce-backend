package com.Group2.Ecommerce.Payment;

import com.Group2.Ecommerce.Common.Exception.ResourceNotFoundException;
import com.Group2.Ecommerce.Order.Order;
import com.Group2.Ecommerce.Order.OrderEmailService;
import com.Group2.Ecommerce.Order.OrderRepository;
import com.Group2.Ecommerce.Order.OrderStatus;
import com.Group2.Ecommerce.Payment.Dto.InitializePaymentRequest;
import com.Group2.Ecommerce.Payment.Dto.InitializePaymentResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProcessedWebhookEventRepository processedWebhookEventRepository;
    private final OrderEmailService orderEmailService;

    @Value("${paystack.secret-key}")
    private String secretKey;

    @Value("${paystack.base-url}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public InitializePaymentResponse initialize(InitializePaymentRequest request, String userEmail) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));

        long amountInKobo = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValueExact();
        String reference = "order_" + order.getId() + "_" + UUID.randomUUID().toString().substring(0, 8);

        RestClient client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + secretKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> body = Map.of(
                "email", userEmail,
                "amount", amountInKobo,
                "reference", reference
        );

        String responseJson = client.post()
                .uri("/transaction/initialize")
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode json = parseJson(responseJson);
        String authorizationUrl = json.path("data").path("authorization_url").asText();

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setReference(reference);
        payment.setAmount(order.getTotalAmount());
        payment.setCurrency("NGN");
        paymentRepository.save(payment);

        return new InitializePaymentResponse(authorizationUrl, reference);
    }

    // Verifies the request genuinely came from Paystack: recompute the
    // HMAC-SHA512 of the raw payload using our secret key, and compare
    // against the signature Paystack sent in the x-paystack-signature header.
    // Without this, anyone could POST a fake "charge.success" event and
    // mark any order as paid for free.
    public boolean isValidSignature(String payload, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            // Constant-time comparison to avoid timing attacks
            return MessageDigest.isEqual(
                    hex.toString().getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public void handleWebhook(String payload) {
        JsonNode event = parseJson(payload);
        String eventType = event.path("event").asText();
        String reference = event.path("data").path("reference").asText();

        if (reference == null || reference.isBlank()) {
            return;
        }

        if (processedWebhookEventRepository.existsById(reference)) {
            return;
        }

        Payment payment = paymentRepository.findByReference(reference)
                .orElse(null);

        if (payment == null) {
            return;
        }

        if ("charge.success".equals(eventType)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.getOrder().setStatus(OrderStatus.PAID);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        paymentRepository.save(payment);
        orderRepository.save(payment.getOrder());

        if ("charge.success".equals(eventType)) {
            orderEmailService.sendConfirmation(payment.getOrder());
        }

        ProcessedWebhookEvent processed = new ProcessedWebhookEvent();
        processed.setReference(reference);
        processedWebhookEventRepository.save(processed);
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Paystack response: " + e.getMessage());
        }
    }
}