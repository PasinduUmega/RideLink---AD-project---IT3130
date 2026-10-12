package com.ridelink.farepayment.repository;

import com.ridelink.farepayment.entity.Fare;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FareRepository extends MongoRepository<Fare, String> {
    Optional<Fare> findByRideId(String rideId);
    List<Fare> findByRiderId(String riderId);
}
