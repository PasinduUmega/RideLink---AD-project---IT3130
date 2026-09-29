package com.ridelink.farepayment.dto;

import com.ridelink.farepayment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/** Lightweight status check for a payment, e.g. for polling from a client. */
@Getter
@Builder
@AllArgsConstructor
public class PaymentStatusResponse {
    private String paymentId;
    private PaymentStatus status;
    private Double amount;
    private String transactionId;
    private String failureReason;
    private String receiptNumber;
    private Instant updatedAt;
}
