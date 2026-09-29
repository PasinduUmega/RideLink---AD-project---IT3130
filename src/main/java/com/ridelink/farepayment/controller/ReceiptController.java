package com.ridelink.farepayment.controller;

import com.ridelink.farepayment.dto.ReceiptResponse;
import com.ridelink.farepayment.service.ReceiptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @GetMapping
    public ResponseEntity<List<ReceiptResponse>> getAll(@RequestParam(required = false) String riderId,
                                                        @RequestParam(required = false) String rideId) {
        if (riderId != null) return ResponseEntity.ok(receiptService.getByRiderId(riderId));
        if (rideId != null) return ResponseEntity.ok(receiptService.getByRideId(rideId));
        return ResponseEntity.ok(receiptService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReceiptResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(receiptService.getById(id));
    }

    @GetMapping("/by-number/{receiptNumber}")
    public ResponseEntity<ReceiptResponse> getByNumber(@PathVariable String receiptNumber) {
        return ResponseEntity.ok(receiptService.getByNumber(receiptNumber));
    }
}
