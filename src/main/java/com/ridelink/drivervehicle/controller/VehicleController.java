package com.ridelink.drivervehicle.controller;

import com.ridelink.drivervehicle.dto.*;
import com.ridelink.drivervehicle.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Exposes vehicle API routes; the service handles database access and business rules. */
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    // Validate the registration details and return the created vehicle with HTTP 201.
    @PostMapping
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody CreateVehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.create(request));
    }

    // Return one driver's vehicles when driverId is supplied, otherwise return all vehicles.
    @GetMapping
    public ResponseEntity<List<VehicleResponse>> getAll(@RequestParam(required = false) String driverId) {
        if (driverId != null) {
            return ResponseEntity.ok(vehicleService.getByDriverId(driverId));
        }
        return ResponseEntity.ok(vehicleService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(vehicleService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponse> update(@PathVariable String id, @Valid @RequestBody UpdateVehicleRequest request) {
        return ResponseEntity.ok(vehicleService.update(id, request));
    }

    // Remove the vehicle document and return HTTP 204 with no response body.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        vehicleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Keep the vehicle record but exclude it from active-vehicle matching.
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<VehicleResponse> deactivate(@PathVariable String id) {
        return ResponseEntity.ok(vehicleService.deactivate(id));
    }
}
