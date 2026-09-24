package com.ridelink.farepayment.service;

import com.ridelink.farepayment.dto.PaymentResponse;
import com.ridelink.farepayment.dto.ProcessPaymentRequest;
import com.ridelink.farepayment.entity.Fare;
import com.ridelink.farepayment.entity.FareStatus;
import com.ridelink.farepayment.entity.Payment;
import com.ridelink.farepayment.entity.PaymentStatus;
import com.ridelink.farepayment.exception.BadRequestException;
import com.ridelink.farepayment.exception.PaymentFailedException;
import com.ridelink.farepayment.exception.ResourceNotFoundException;
import com.ridelink.farepayment.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final FareService fareService;

    public PaymentService(PaymentRepository paymentRepository, FareService fareService) {
        this.paymentRepository = paymentRepository;
        this.fareService = fareService;
    }

    public PaymentResponse process(ProcessPaymentRequest req) {
        Fare fare = fareService.findOr404(req.getFareId());

        if (fare.getStatus() == FareStatus.PAID) {
            throw new BadRequestException("This fare has already been paid");
        }
        if (fare.getStatus() == FareStatus.VOID) {
            throw new BadRequestException("This fare has been voided and cannot be paid");
        }

        Instant now = Instant.now();
        Payment payment = Payment.builder()
                .fareId(fare.getId())
                .riderId(req.getRiderId())
                .method(req.getMethod())
                .amount(fare.getTotalFare())
                .status(PaymentStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Simulated payment gateway call. In production this would call out to
        // Stripe/Braintree/etc. and handle async webhooks instead of an inline result.
        boolean success = simulateGateway();

        if (success) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setTransactionId("txn_" + UUID.randomUUID());
            payment.setPaidAt(Instant.now());
            payment = paymentRepository.save(payment);
            fareService.markPaid(fare.getId());
            return PaymentResponse.from(payment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentFailedException("Payment could not be processed. Please try again.");
        }
    }

    public PaymentResponse getById(String id) {
        return PaymentResponse.from(findOr404(id));
    }

    public List<PaymentResponse> getByFareId(String fareId) {
        return paymentRepository.findByFareId(fareId).stream().map(PaymentResponse::from).toList();
    }

    public List<PaymentResponse> getByRiderId(String riderId) {
        return paymentRepository.findByRiderId(riderId).stream().map(PaymentResponse::from).toList();
    }

    public List<PaymentResponse> getAll() {
        return paymentRepository.findAll().stream().map(PaymentResponse::from).toList();
    }

    public PaymentResponse refund(String id) {
        Payment payment = findOr404(id);

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BadRequestException("Only completed payments can be refunded");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setUpdatedAt(Instant.now());
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    private Payment findOr404(String id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    private boolean simulateGateway() {
        // Deterministic success for demo purposes - swap in a real gateway SDK call.
        return true;
    }
}
