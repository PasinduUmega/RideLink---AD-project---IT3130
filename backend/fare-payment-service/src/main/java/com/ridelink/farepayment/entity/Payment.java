package com.ridelink.farepayment.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    private String id;

    @Indexed
    private String fareId;

    private String riderId;

    @Indexed
    private String rideId;

    private PaymentMethod method;

    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    private Double amount;

    private String transactionId;

    private String failureReason;

    private Instant refundedAt;

    private Instant paidAt;

    private Instant createdAt;

    private Instant updatedAt;
}
