package com.ridelink.drivervehicle.service;

import com.ridelink.drivervehicle.dto.*;
import com.ridelink.drivervehicle.entity.Vehicle;
import com.ridelink.drivervehicle.exception.BadRequestException;
import com.ridelink.drivervehicle.exception.ResourceNotFoundException;
import com.ridelink.drivervehicle.repository.DriverRepository;
import com.ridelink.drivervehicle.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public VehicleService(VehicleRepository vehicleRepository, DriverRepository driverRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    public VehicleResponse create(CreateVehicleRequest req) {
        if (vehicleRepository.existsByPlateNumber(req.getPlateNumber())) {
            throw new BadRequestException("A vehicle with this plate number already exists");
        }

        driverRepository.findById(req.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + req.getDriverId()));

        Instant now = Instant.now();
        Vehicle vehicle = Vehicle.builder()
                .driverId(req.getDriverId())
                .make(req.getMake())
                .model(req.getModel())
                .year(req.getYear())
                .plateNumber(req.getPlateNumber())
                .color(req.getColor())
                .capacity(req.getCapacity() != null ? req.getCapacity() : 4)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    public List<VehicleResponse> getAll() {
        return vehicleRepository.findAll().stream().map(VehicleResponse::from).toList();
    }

    public List<VehicleResponse> getByDriverId(String driverId) {
        return vehicleRepository.findByDriverId(driverId).stream().map(VehicleResponse::from).toList();
    }

    public VehicleResponse getById(String id) {
        return VehicleResponse.from(findOr404(id));
    }

    public VehicleResponse update(String id, UpdateVehicleRequest req) {
        Vehicle vehicle = findOr404(id);
        if (req.getMake() != null) vehicle.setMake(req.getMake());
        if (req.getModel() != null) vehicle.setModel(req.getModel());
        if (req.getYear() != null) vehicle.setYear(req.getYear());
        if (req.getColor() != null) vehicle.setColor(req.getColor());
        if (req.getCapacity() != null) vehicle.setCapacity(req.getCapacity());
        vehicle.setUpdatedAt(Instant.now());
        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    public void delete(String id) {
        vehicleRepository.delete(findOr404(id));
    }

    public VehicleResponse deactivate(String id) {
        Vehicle vehicle = findOr404(id);
        vehicle.setActive(false);
        vehicle.setUpdatedAt(Instant.now());
        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    private Vehicle findOr404(String id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
    }
}
