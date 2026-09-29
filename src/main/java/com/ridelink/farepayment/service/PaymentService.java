package com.ridelink.farepayment.service;

import com.ridelink.farepayment.dto.PaymentResponse;
import com.ridelink.farepayment.dto.PaymentStatusResponse;
import com.ridelink.farepayment.dto.ProcessPaymentRequest;
import com.ridelink.farepayment.dto.ReceiptResponse;
import com.ridelink.farepayment.entity.Fare;
import com.ridelink.farepayment.entity.FareStatus;
import com.ridelink.farepayment.entity.Payment;
import com.ridelink.farepayment.entity.PaymentStatus;
import com.ridelink.farepayment.entity.Receipt;
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
    private final ReceiptService receiptService;

    public PaymentService(PaymentRepository paymentRepository, FareService fareService, ReceiptService receiptService) {
        this.paymentRepository = paymentRepository;
        this.fareService = fareService;
        this.receiptService = receiptService;
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
                .rideId(fare.getRideId())
                .riderId(req.getRiderId())
                .method(req.getMethod())
                .amount(fare.getTotalFare())
                .status(PaymentStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Simulated payment gateway call. In production this would call out to
        // Stripe/Braintree/etc. and handle async webhooks instead of an inline result.
        boolean success = simulateGateway(req);

        if (success) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setTransactionId("txn_" + UUID.randomUUID());
            payment.setPaidAt(Instant.now());
            payment = paymentRepository.save(payment);
            fareService.markPaid(fare.getId());
            receiptService.generate(payment, fare);
            return PaymentResponse.from(payment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Declined by simulated gateway");
            paymentRepository.save(payment);
            throw new PaymentFailedException("Payment could not be processed. Please try again.");
        }
    }

    public PaymentResponse getById(String id) {
        return PaymentResponse.from(findOr404(id));
    }

    public PaymentStatusResponse getStatus(String id) {
        Payment p = findOr404(id);
        return PaymentStatusResponse.builder()
                .paymentId(p.getId())
                .status(p.getStatus())
                .amount(p.getAmount())
                .transactionId(p.getTransactionId())
                .failureReason(p.getFailureReason())
                .receiptNumber(receiptService.findByPaymentId(p.getId()).map(Receipt::getReceiptNumber).orElse(null))
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    /** Returns the payment's receipt, issuing it first if it is missing (e.g. payments made before receipts existed). */
    public ReceiptResponse getReceipt(String id) {
        Payment payment = findOr404(id);
        if (payment.getStatus() != PaymentStatus.COMPLETED && payment.getStatus() != PaymentStatus.REFUNDED) {
            throw new BadRequestException("No receipt: payment is " + payment.getStatus());
        }
        Receipt receipt = receiptService.findByPaymentId(id)
                .orElseGet(() -> receiptService.generate(payment, fareService.findOr404(payment.getFareId())));
        return ReceiptResponse.from(receipt);
    }

    public List<PaymentResponse> getByFareId(String fareId) {
        return paymentRepository.findByFareId(fareId).stream().map(PaymentResponse::from).toList();
    }

    public List<PaymentResponse> getByRiderId(String riderId) {
        return paymentRepository.findByRiderId(riderId).stream().map(PaymentResponse::from).toList();
    }

    public List<PaymentResponse> getByRideId(String rideId) {
        return paymentRepository.findByRideId(rideId).stream().map(PaymentResponse::from).toList();
    }

    public List<PaymentResponse> getAll() {
        return paymentRepository.findAll().stream().map(PaymentResponse::from).toList();
    }

    public PaymentResponse refund(String id) {
        Payment payment = findOr404(id);

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BadRequestException("Only completed payments can be refunded");
        }

        Instant now = Instant.now();
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(now);
        payment.setUpdatedAt(now);
        payment = paymentRepository.save(payment);
        receiptService.markRefunded(payment.getId(), now);
        return PaymentResponse.from(payment);
    }

    private Payment findOr404(String id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    private boolean simulateGateway(ProcessPaymentRequest req) {
        // Succeeds unless the caller explicitly asks for a decline (to demo the FAILED path).
        return !req.isSimulateFailure();
    }
}
