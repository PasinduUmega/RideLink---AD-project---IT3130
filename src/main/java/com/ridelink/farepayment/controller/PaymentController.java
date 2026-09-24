package com.ridelink.farepayment.controller;

import com.ridelink.farepayment.dto.PaymentResponse;
import com.ridelink.farepayment.dto.ProcessPaymentRequest;
import com.ridelink.farepayment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> process(@Valid @RequestBody ProcessPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.process(request));
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAll(
            @RequestParam(required = false) String riderId,
            @RequestParam(required = false) String fareId) {

        if (riderId != null) return ResponseEntity.ok(paymentService.getByRiderId(riderId));
        if (fareId != null) return ResponseEntity.ok(paymentService.getByFareId(fareId));
        return ResponseEntity.ok(paymentService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refund(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.refund(id));
    }
}
