package com.ridelink.farepayment.repository;

import com.ridelink.farepayment.entity.Receipt;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ReceiptRepository extends MongoRepository<Receipt, String> {
    Optional<Receipt> findByPaymentId(String paymentId);
    Optional<Receipt> findByReceiptNumber(String receiptNumber);
    List<Receipt> findByRideId(String rideId);
    List<Receipt> findByRiderId(String riderId);
}
