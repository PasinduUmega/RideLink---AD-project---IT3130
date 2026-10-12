package com.ridelink.farepayment.service;

import com.ridelink.farepayment.dto.CalculateFareRequest;
import com.ridelink.farepayment.dto.EstimateFareRequest;
import com.ridelink.farepayment.dto.FareEstimateResponse;
import com.ridelink.farepayment.dto.FareResponse;
import com.ridelink.farepayment.entity.Fare;
import com.ridelink.farepayment.entity.FareStatus;
import com.ridelink.farepayment.exception.BadRequestException;
import com.ridelink.farepayment.exception.ResourceNotFoundException;
import com.ridelink.farepayment.repository.FareRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class FareService {

    private final FareRepository fareRepository;
    private final FareCalculator fareCalculator;

    public FareService(FareRepository fareRepository, FareCalculator fareCalculator) {
        this.fareRepository = fareRepository;
        this.fareCalculator = fareCalculator;
    }

    /** Quote before/at ride request time - nothing is persisted. */
    public FareEstimateResponse estimate(EstimateFareRequest req) {
        return FareEstimateResponse.from(fareCalculator.calculate(req.getDistanceKm(), req.getDurationMinutes()));
    }

    public FareResponse calculate(CalculateFareRequest req) {
        if (fareRepository.findByRideId(req.getRideId()).isPresent()) {
            throw new BadRequestException("A fare has already been calculated for rideId: " + req.getRideId());
        }

        FareCalculator.Breakdown b = fareCalculator.calculate(req.getDistanceKm(), req.getDurationMinutes());

        Instant now = Instant.now();
        Fare fare = Fare.builder()
                .rideId(req.getRideId())
                .riderId(req.getRiderId())
                .baseFare(b.baseFare())
                .distanceKm(b.distanceKm())
                .distanceFare(b.distanceFare())
                .durationMinutes(b.durationMinutes())
                .timeFare(b.timeFare())
                .subtotal(b.subtotal())
                .minimumFareApplied(b.minimumFareApplied())
                .totalFare(b.totalFare())
                .currency(b.currency())
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
}
