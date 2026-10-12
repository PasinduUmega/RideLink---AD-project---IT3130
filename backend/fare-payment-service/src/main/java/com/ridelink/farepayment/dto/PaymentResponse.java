package com.ridelink.farepayment.dto;

import com.ridelink.farepayment.entity.Payment;
import com.ridelink.farepayment.entity.PaymentMethod;
import com.ridelink.farepayment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class PaymentResponse {
    private String id;
    private String fareId;
    private String riderId;
    private String rideId;
    private PaymentMethod method;
    private PaymentStatus status;
    private Double amount;
    private String transactionId;
    private String failureReason;
    private Instant paidAt;
    private Instant refundedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static PaymentResponse from(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .fareId(p.getFareId())
                .riderId(p.getRiderId())
                .rideId(p.getRideId())
                .method(p.getMethod())
                .status(p.getStatus())
                .amount(p.getAmount())
                .transactionId(p.getTransactionId())
                .failureReason(p.getFailureReason())
                .paidAt(p.getPaidAt())
                .refundedAt(p.getRefundedAt())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
