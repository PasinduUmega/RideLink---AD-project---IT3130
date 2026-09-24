package com.ridelink.farepayment.repository;

import com.ridelink.farepayment.entity.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    List<Payment> findByFareId(String fareId);
    List<Payment> findByRiderId(String riderId);
}
