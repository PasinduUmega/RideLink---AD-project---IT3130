package com.ridelink.farepayment.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Immutable snapshot of a successful payment and the fare it settled, so the receipt stays
 * correct even if the fare rule or fare record changes later. Only the refund fields change.
 */
@Document(collection = "receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receipt {

    @Id
    private String id;

    /** Human-friendly number, e.g. RL-20260925-4F7A1C. */
    @Indexed(unique = true)
    private String receiptNumber;

    @Indexed(unique = true)
    private String paymentId;

    private String fareId;

    @Indexed
    private String rideId;

    @Indexed
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
}
