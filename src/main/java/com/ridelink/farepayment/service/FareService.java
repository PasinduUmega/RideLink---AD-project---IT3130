package com.ridelink.farepayment.service;

import com.ridelink.farepayment.dto.CalculateFareRequest;
import com.ridelink.farepayment.dto.FareResponse;
import com.ridelink.farepayment.entity.Fare;
import com.ridelink.farepayment.entity.FareStatus;
import com.ridelink.farepayment.exception.BadRequestException;
import com.ridelink.farepayment.exception.ResourceNotFoundException;
import com.ridelink.farepayment.repository.FareRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class FareService {

    private final FareRepository fareRepository;
    private final double baseFare;
    private final double perKmRate;
    private final String currency;

    public FareService(FareRepository fareRepository,
                        @Value("${fare.base-fare}") double baseFare,
                        @Value("${fare.per-km-rate}") double perKmRate,
                        @Value("${fare.currency}") String currency) {
        this.fareRepository = fareRepository;
        this.baseFare = baseFare;
        this.perKmRate = perKmRate;
        this.currency = currency;
    }

    public FareResponse calculate(CalculateFareRequest req) {
        if (fareRepository.findByRideId(req.getRideId()).isPresent()) {
            throw new BadRequestException("A fare has already been calculated for rideId: " + req.getRideId());
        }

        double distanceFare = round(req.getDistanceKm() * perKmRate);
        double total = round(baseFare + distanceFare);

        Instant now = Instant.now();
        Fare fare = Fare.builder()
                .rideId(req.getRideId())
                .riderId(req.getRiderId())
                .baseFare(baseFare)
                .distanceKm(req.getDistanceKm())
                .distanceFare(distanceFare)
                .totalFare(total)
                .currency(currency)
                .status(FareStatus.INVOICED)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return FareResponse.from(fareRepository.save(fare));
    }

    public FareResponse getById(String id) {
        return FareResponse.from(findOr404(id));
    }

    public FareResponse getByRideId(String rideId) {
        return fareRepository.findByRideId(rideId)
                .map(FareResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No fare found for rideId: " + rideId));
    }

    public List<FareResponse> getByRiderId(String riderId) {
        return fareRepository.findByRiderId(riderId).stream().map(FareResponse::from).toList();
    }

    public List<FareResponse> getAll() {
        return fareRepository.findAll().stream().map(FareResponse::from).toList();
    }

    public void markPaid(String fareId) {
        Fare fare = findOr404(fareId);
        fare.setStatus(FareStatus.PAID);
        fare.setUpdatedAt(Instant.now());
        fareRepository.save(fare);
    }

    public void markVoid(String fareId) {
        Fare fare = findOr404(fareId);
        fare.setStatus(FareStatus.VOID);
        fare.setUpdatedAt(Instant.now());
        fareRepository.save(fare);
    }

    Fare findOr404(String id) {
        return fareRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fare not found with id: " + id));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
