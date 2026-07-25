package com.Group2.Ecommerce.Payment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Prevents double-processing if Paystack retries the same webhook event
// (which it does — retries are expected, not a bug on their end).
@Entity
@Table(name = "processed_webhook_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedWebhookEvent {

    @Id
    private String reference; // Paystack transaction reference used as the idempotency key

    @Column(nullable = false)
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        this.processedAt = LocalDateTime.now();
    }
}