package com.ridelink.farepayment.dto;

import com.ridelink.farepayment.entity.PaymentMethod;
import com.ridelink.farepayment.entity.PaymentStatus;
import com.ridelink.farepayment.entity.Receipt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class ReceiptResponse {
    private String id;
    private String receiptNumber;
    private String paymentId;
    private String fareId;
    private String rideId;
    private String riderId;
    private PaymentMethod method;
    private String transactionId;
    private Double baseFare;
    private Double distanceKm;
    private Double distanceFare;
    private Integer durationMinutes;
    private Double timeFare;
    private boolean minimumFareApplied;
    private Double totalPaid;
    private String currency;
    private PaymentStatus paymentStatus;
    private Instant paidAt;
    private Instant issuedAt;
    private Instant refundedAt;

    public static ReceiptResponse from(Receipt r) {
        return ReceiptResponse.builder()
                .id(r.getId())
                .receiptNumber(r.getReceiptNumber())
                .paymentId(r.getPaymentId())
                .fareId(r.getFareId())
                .rideId(r.getRideId())
                .riderId(r.getRiderId())
                .method(r.getMethod())
                .transactionId(r.getTransactionId())
                .baseFare(r.getBaseFare())
                .distanceKm(r.getDistanceKm())
                .distanceFare(r.getDistanceFare())
                .durationMinutes(r.getDurationMinutes())
                .timeFare(r.getTimeFare())
                .minimumFareApplied(r.isMinimumFareApplied())
                .totalPaid(r.getTotalPaid())
                .currency(r.getCurrency())
                .paymentStatus(r.getPaymentStatus())
                .paidAt(r.getPaidAt())
                .issuedAt(r.getIssuedAt())
                .refundedAt(r.getRefundedAt())
                .build();
    }
}
