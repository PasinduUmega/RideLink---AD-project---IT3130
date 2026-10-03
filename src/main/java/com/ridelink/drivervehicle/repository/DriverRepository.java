package com.ridelink.drivervehicle.repository;

import com.ridelink.drivervehicle.entity.AvailabilityStatus;
import com.ridelink.drivervehicle.entity.Driver;
import com.ridelink.drivervehicle.entity.DriverStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data supplies CRUD operations and derives MongoDB queries from these method names. */
public interface DriverRepository extends MongoRepository<Driver, String> {
    Optional<Driver> findByUserId(String userId);
    boolean existsByLicenseNumber(String licenseNumber);
    List<Driver> findByStatus(DriverStatus status);
    // Both conditions must match before the service applies area, vehicle and distance filters.
    List<Driver> findByStatusAndAvailability(DriverStatus status, AvailabilityStatus availability);
}
