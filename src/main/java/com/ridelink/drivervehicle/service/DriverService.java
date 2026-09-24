package com.ridelink.drivervehicle.service;

import com.ridelink.drivervehicle.dto.*;
import com.ridelink.drivervehicle.entity.Driver;
import com.ridelink.drivervehicle.entity.DriverStatus;
import com.ridelink.drivervehicle.exception.BadRequestException;
import com.ridelink.drivervehicle.exception.ResourceNotFoundException;
import com.ridelink.drivervehicle.repository.DriverRepository;
import com.ridelink.drivervehicle.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    public DriverService(DriverRepository driverRepository, VehicleRepository vehicleRepository) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public DriverResponse create(CreateDriverRequest req) {
        if (driverRepository.existsByLicenseNumber(req.getLicenseNumber())) {
            throw new BadRequestException("A driver with this license number already exists");
        }
        driverRepository.findByUserId(req.getUserId()).ifPresent(d -> {
            throw new BadRequestException("This user already has a driver profile");
        });

        Instant now = Instant.now();
        Driver driver = Driver.builder()
                .userId(req.getUserId())
                .licenseNumber(req.getLicenseNumber())
                .licenseExpiry(req.getLicenseExpiry())
                .status(DriverStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toResponse(driverRepository.save(driver));
    }

    public List<DriverResponse> getAll() {
        return driverRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<DriverResponse> getByStatus(DriverStatus status) {
        return driverRepository.findByStatus(status).stream().map(this::toResponse).toList();
    }

    public DriverResponse getById(String id) {
        return toResponse(findOr404(id));
    }

    public DriverResponse getByUserId(String userId) {
        return driverRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No driver profile for userId: " + userId));
    }

    public DriverResponse update(String id, UpdateDriverRequest req) {
        Driver driver = findOr404(id);
        if (req.getLicenseNumber() != null) driver.setLicenseNumber(req.getLicenseNumber());
        if (req.getLicenseExpiry() != null) driver.setLicenseExpiry(req.getLicenseExpiry());
        driver.setUpdatedAt(Instant.now());
        return toResponse(driverRepository.save(driver));
    }

    public DriverResponse updateStatus(String id, UpdateDriverStatusRequest req) {
        Driver driver = findOr404(id);
        driver.setStatus(req.getStatus());
        driver.setUpdatedAt(Instant.now());
        return toResponse(driverRepository.save(driver));
    }

    public void delete(String id) {
        Driver driver = findOr404(id);
        vehicleRepository.findByDriverId(driver.getId()).forEach(vehicleRepository::delete);
        driverRepository.delete(driver);
    }

    private DriverResponse toResponse(Driver driver) {
        List<VehicleResponse> vehicles = vehicleRepository.findByDriverId(driver.getId()).stream()
                .map(VehicleResponse::from)
                .toList();
        return DriverResponse.from(driver, vehicles);
    }

    private Driver findOr404(String id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
    }
}
