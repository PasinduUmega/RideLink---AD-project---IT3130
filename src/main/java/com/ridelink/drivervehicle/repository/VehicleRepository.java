package com.ridelink.drivervehicle.repository;

import com.ridelink.drivervehicle.entity.Vehicle;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data implements vehicle persistence and derives owner/plate queries from method names. */
public interface VehicleRepository extends MongoRepository<Vehicle, String> {
    List<Vehicle> findByDriverId(String driverId);
    Optional<Vehicle> findByPlateNumber(String plateNumber);
    boolean existsByPlateNumber(String plateNumber);
}
