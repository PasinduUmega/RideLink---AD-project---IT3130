package com.ridelink.drivervehicle.controller;

import com.ridelink.drivervehicle.dto.*;
import com.ridelink.drivervehicle.entity.DriverStatus;
import com.ridelink.drivervehicle.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    public ResponseEntity<DriverResponse> create(@Valid @RequestBody CreateDriverRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(driverService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<DriverResponse>> getAll(@RequestParam(required = false) DriverStatus status) {
        if (status != null) {
            return ResponseEntity.ok(driverService.getByStatus(status));
        }
        return ResponseEntity.ok(driverService.getAll());
    }

    /** Eligible drivers for matching: APPROVED, AVAILABLE, with a suitable active vehicle. */
    @GetMapping("/available")
    public ResponseEntity<List<DriverResponse>> getAvailable(
            @RequestParam(required = false) String serviceArea,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) Integer minCapacity) {
        return ResponseEntity.ok(driverService.findAvailable(serviceArea, lat, lng, radiusKm, minCapacity));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(driverService.getById(id));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<DriverResponse> getByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(driverService.getByUserId(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DriverResponse> update(@PathVariable String id, @Valid @RequestBody UpdateDriverRequest request) {
        return ResponseEntity.ok(driverService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DriverResponse> updateStatus(@PathVariable String id, @Valid @RequestBody UpdateDriverStatusRequest request) {
        return ResponseEntity.ok(driverService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<DriverResponse> updateAvailability(@PathVariable String id,
                                                             @Valid @RequestBody UpdateAvailabilityRequest request) {
        return ResponseEntity.ok(driverService.updateAvailability(id, request));
    }

    @PutMapping("/{id}/location")
    public ResponseEntity<DriverResponse> updateLocation(@PathVariable String id,
                                                         @Valid @RequestBody UpdateLocationRequest request) {
        return ResponseEntity.ok(driverService.updateLocation(id, request));
    }

    @PostMapping("/{id}/location/simulate")
    public ResponseEntity<DriverResponse> simulateLocation(@PathVariable String id) {
        return ResponseEntity.ok(driverService.simulateLocation(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        driverService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
