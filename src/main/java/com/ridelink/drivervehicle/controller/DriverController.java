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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        driverService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
