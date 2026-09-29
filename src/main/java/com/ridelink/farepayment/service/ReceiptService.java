package com.ridelink.farepayment.service;

import com.ridelink.farepayment.dto.ReceiptResponse;
import com.ridelink.farepayment.entity.Fare;
import com.ridelink.farepayment.entity.Payment;
import com.ridelink.farepayment.entity.PaymentStatus;
import com.ridelink.farepayment.entity.Receipt;
import com.ridelink.farepayment.exception.ResourceNotFoundException;
import com.ridelink.farepayment.repository.ReceiptRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReceiptService {

    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final ReceiptRepository receiptRepository;

    public ReceiptService(ReceiptRepository receiptRepository) {
        this.receiptRepository = receiptRepository;
    }

    /** Idempotent: returns the existing receipt if one was already issued for this payment. */
    public Receipt generate(Payment payment, Fare fare) {
        return receiptRepository.findByPaymentId(payment.getId()).orElseGet(() -> {
            Instant now = Instant.now();
            Receipt receipt = Receipt.builder()
                    .receiptNumber(newReceiptNumber(now))
                    .paymentId(payment.getId())
                    .fareId(fare.getId())
                    .rideId(fare.getRideId())
                    .riderId(payment.getRiderId())
                    .method(payment.getMethod())
                    .transactionId(payment.getTransactionId())
                    .baseFare(fare.getBaseFare())
                    .distanceKm(fare.getDistanceKm())
                    .distanceFare(fare.getDistanceFare())
                    .durationMinutes(fare.getDurationMinutes())
                    .timeFare(fare.getTimeFare())
                    .minimumFareApplied(fare.isMinimumFareApplied())
                    .totalPaid(payment.getAmount())
                    .currency(fare.getCurrency())
                    .paymentStatus(payment.getStatus())
                    .paidAt(payment.getPaidAt())
                    .issuedAt(now)
                    .build();
            return receiptRepository.save(receipt);
        });
    }

    public void markRefunded(String paymentId, Instant refundedAt) {
        receiptRepository.findByPaymentId(paymentId).ifPresent(r -> {
            r.setPaymentStatus(PaymentStatus.REFUNDED);
            r.setRefundedAt(refundedAt);
            receiptRepository.save(r);
        });
    }

    public Optional<Receipt> findByPaymentId(String paymentId) {
        return receiptRepository.findByPaymentId(paymentId);
    }

    public ReceiptResponse getById(String id) {
        return receiptRepository.findById(id)
                .map(ReceiptResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with id: " + id));
    }

    public ReceiptResponse getByNumber(String receiptNumber) {
        return receiptRepository.findByReceiptNumber(receiptNumber)
                .map(ReceiptResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with number: " + receiptNumber));
    }

    public List<ReceiptResponse> getByRideId(String rideId) {
        return receiptRepository.findByRideId(rideId).stream().map(ReceiptResponse::from).toList();
    }

    public List<ReceiptResponse> getByRiderId(String riderId) {
        return receiptRepository.findByRiderId(riderId).stream().map(ReceiptResponse::from).toList();
    }

    public List<ReceiptResponse> getAll() {
        return receiptRepository.findAll().stream().map(ReceiptResponse::from).toList();
    }

    private static String newReceiptNumber(Instant when) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "RL-" + NUMBER_DATE.format(when) + "-" + suffix;
    }
}
