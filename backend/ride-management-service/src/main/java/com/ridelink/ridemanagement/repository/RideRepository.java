package com.ridelink.ridemanagement.repository;

import com.ridelink.ridemanagement.entity.Ride;
import com.ridelink.ridemanagement.entity.RideStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RideRepository extends MongoRepository<Ride, String> {
    List<Ride> findByRiderId(String riderId);
    List<Ride> findByDriverId(String driverId);
    List<Ride> findByStatus(RideStatus status);
    List<Ride> findByDriverIdAndStatus(String driverId, RideStatus status);
}
