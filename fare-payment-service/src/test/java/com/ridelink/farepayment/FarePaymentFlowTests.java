package com.ridelink.farepayment;

import com.ridelink.farepayment.dto.*;
import com.ridelink.farepayment.entity.FareStatus;
import com.ridelink.farepayment.entity.PaymentMethod;
import com.ridelink.farepayment.entity.PaymentStatus;
import com.ridelink.farepayment.exception.BadRequestException;
import com.ridelink.farepayment.exception.PaymentFailedException;
import com.ridelink.farepayment.service.FareService;
import com.ridelink.farepayment.service.PaymentService;
import com.ridelink.farepayment.service.ReceiptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FarePaymentFlowTests extends AbstractMongoTest {

    @Autowired
    private FareService fareService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ReceiptService receiptService;

    private EstimateFareRequest estimateReq(double km, Integer minutes) {
        EstimateFareRequest req = new EstimateFareRequest();
        req.setDistanceKm(km);
        req.setDurationMinutes(minutes);
        return req;
    }

    private FareResponse calculate(double km, Integer minutes) {
        CalculateFareRequest req = new CalculateFareRequest();
        req.setRideId("ride-" + UUID.randomUUID());
        req.setRiderId("rider-1");
        req.setDistanceKm(km);
        req.setDurationMinutes(minutes);
        return fareService.calculate(req);
    }

    private ProcessPaymentRequest payReq(String fareId, boolean fail) {
        ProcessPaymentRequest req = new ProcessPaymentRequest();
        req.setFareId(fareId);
        req.setRiderId("rider-1");
        req.setMethod(PaymentMethod.CARD);
        req.setSimulateFailure(fail);
        return req;
    }

    @Test
    void estimateAppliesDocumentedRule() {
        // 2.50 + 10 x 1.20 + 25 x 0.20 = 19.50
        FareEstimateResponse e = fareService.estimate(estimateReq(10, 25));
        assertEquals(12.0, e.getDistanceFare());
        assertEquals(5.0, e.getTimeFare());
        assertEquals(19.5, e.getTotalFare());
        assertFalse(e.isDurationEstimated());
        assertFalse(e.isMinimumFareApplied());
    }

    @Test
    void estimateDerivesDurationFromDistanceWhenOmitted() {
        // 15 km at 30 km/h = 30 min -> 2.50 + 18.00 + 6.00 = 26.50
        FareEstimateResponse e = fareService.estimate(estimateReq(15, null));
        assertEquals(30, e.getDurationMinutes());
        assertTrue(e.isDurationEstimated());
        assertEquals(26.5, e.getTotalFare());
    }

    @Test
    void minimumFareAppliesToShortTrips() {
        // 2.50 + 1 x 1.20 + 2 x 0.20 = 4.10 -> minimum 5.00
        FareEstimateResponse e = fareService.estimate(estimateReq(1, 2));
        assertEquals(4.1, e.getSubtotal());
        assertTrue(e.isMinimumFareApplied());
        assertEquals(5.0, e.getTotalFare());
    }

    @Test
    void finalFareMatchesEstimateForSameInputs() {
        FareResponse fare = calculate(10, 25);
        assertEquals(19.5, fare.getTotalFare());
        assertEquals(FareStatus.INVOICED, fare.getStatus());
    }

    @Test
    void successfulPaymentIssuesReceiptAndRefundUpdatesIt() {
        FareResponse fare = calculate(10, 25);
        PaymentResponse payment = paymentService.process(payReq(fare.getId(), false));
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals(fare.getRideId(), payment.getRideId());
        assertEquals(FareStatus.PAID, fareService.getById(fare.getId()).getStatus());

        ReceiptResponse receipt = paymentService.getReceipt(payment.getId());
        assertTrue(receipt.getReceiptNumber().matches("RL-\\d{8}-[0-9A-F]{6}"));
        assertEquals(19.5, receipt.getTotalPaid());
        assertEquals(5.0, receipt.getTimeFare());
        assertEquals(fare.getRideId(), receipt.getRideId());
        assertEquals(receipt.getId(), receiptService.getByNumber(receipt.getReceiptNumber()).getId());
        assertEquals(1, receiptService.getByRideId(fare.getRideId()).size());

        PaymentStatusResponse status = paymentService.getStatus(payment.getId());
        assertEquals(PaymentStatus.COMPLETED, status.getStatus());
        assertEquals(receipt.getReceiptNumber(), status.getReceiptNumber());

        paymentService.refund(payment.getId());
        ReceiptResponse refunded = paymentService.getReceipt(payment.getId());
        assertEquals(PaymentStatus.REFUNDED, refunded.getPaymentStatus());
        assertNotNull(refunded.getRefundedAt());
        assertEquals(receipt.getReceiptNumber(), refunded.getReceiptNumber());
    }

    @Test
    void failedPaymentIsRecordedWithoutReceipt() {
        FareResponse fare = calculate(5, null);
        assertThrows(PaymentFailedException.class, () -> paymentService.process(payReq(fare.getId(), true)));

        PaymentResponse failed = paymentService.getByFareId(fare.getId()).get(0);
        assertEquals(PaymentStatus.FAILED, failed.getStatus());
        assertEquals("Declined by simulated gateway", paymentService.getStatus(failed.getId()).getFailureReason());
        assertThrows(BadRequestException.class, () -> paymentService.getReceipt(failed.getId()));

        // Fare is still payable after a failed attempt.
        assertEquals(PaymentStatus.COMPLETED, paymentService.process(payReq(fare.getId(), false)).getStatus());
    }
}
