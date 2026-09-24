package com.ridelink.farepayment.controller;

import com.ridelink.farepayment.dto.CalculateFareRequest;
import com.ridelink.farepayment.dto.FareResponse;
import com.ridelink.farepayment.service.FareService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fares")
public class FareController {

    private final FareService fareService;

    public FareController(FareService fareService) {
        this.fareService = fareService;
    }

    @PostMapping
    public ResponseEntity<FareResponse> calculate(@Valid @RequestBody CalculateFareRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fareService.calculate(request));
    }

    @GetMapping
    public ResponseEntity<List<FareResponse>> getAll(@RequestParam(required = false) String riderId) {
        if (riderId != null) {
            return ResponseEntity.ok(fareService.getByRiderId(riderId));
        }
        return ResponseEntity.ok(fareService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FareResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(fareService.getById(id));
    }

    @GetMapping("/by-ride/{rideId}")
    public ResponseEntity<FareResponse> getByRideId(@PathVariable String rideId) {
        return ResponseEntity.ok(fareService.getByRideId(rideId));
    }

    @PatchMapping("/{id}/void")
    public ResponseEntity<Void> voidFare(@PathVariable String id) {
        fareService.markVoid(id);
        return ResponseEntity.noContent().build();
    }
}
