package com.ridelink.ridemanagement.controller;

import com.ridelink.ridemanagement.dto.*;
import com.ridelink.ridemanagement.entity.RideStatus;
import com.ridelink.ridemanagement.service.RideService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping
    public ResponseEntity<RideResponse> requestRide(@Valid @RequestBody CreateRideRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rideService.requestRide(request));
    }

    @GetMapping
    public ResponseEntity<List<RideResponse>> getAll(
            @RequestParam(required = false) String riderId,
            @RequestParam(required = false) String driverId,
            @RequestParam(required = false) RideStatus status) {

        if (riderId != null) return ResponseEntity.ok(rideService.getByRider(riderId));
        if (driverId != null) return ResponseEntity.ok(rideService.getByDriver(driverId));
        if (status != null) return ResponseEntity.ok(rideService.getByStatus(status));
        return ResponseEntity.ok(rideService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RideResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(rideService.getById(id));
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<RideResponse> accept(@PathVariable String id, @Valid @RequestBody AcceptRideRequest request) {
        return ResponseEntity.ok(rideService.accept(id, request));
    }

    @PatchMapping("/{id}/start")
    public ResponseEntity<RideResponse> start(@PathVariable String id) {
        return ResponseEntity.ok(rideService.start(id));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<RideResponse> complete(@PathVariable String id) {
        return ResponseEntity.ok(rideService.complete(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<RideResponse> cancel(@PathVariable String id, @RequestBody(required = false) CancelRideRequest request) {
        return ResponseEntity.ok(rideService.cancel(id, request));
    }
}
