package com.Group2.Ecommerce.Payment;

import com.Group2.Ecommerce.Common.ApiResponse;
import com.Group2.Ecommerce.Payment.Dto.InitializePaymentRequest;
import com.Group2.Ecommerce.Payment.Dto.InitializePaymentResponse;
import com.Group2.Ecommerce.Payment.Dto.PaymentVerifyResponse;
import com.Group2.Ecommerce.User.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initialize")
    public ApiResponse<InitializePaymentResponse> initialize(
            @Valid @RequestBody InitializePaymentRequest request,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        return ApiResponse.success(paymentService.initialize(request, currentUser.getEmail()));
    }

    // Called directly by Paystack's servers — no JWT. Signature verified
    // against the raw payload before any processing happens.
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader("x-paystack-signature") String signature) {

        if (!paymentService.isValidSignature(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        paymentService.handleWebhook(payload);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/verify/{reference}")
    public ApiResponse<PaymentVerifyResponse> verify(@PathVariable String reference) {
        return ApiResponse.success(paymentService.verify(reference));
    }
}