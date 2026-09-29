package com.ridelink.ridemanagement.service;

import com.ridelink.ridemanagement.dto.*;
import com.ridelink.ridemanagement.entity.Ride;
import com.ridelink.ridemanagement.entity.RideStatus;
import com.ridelink.ridemanagement.exception.InvalidRideStateException;
import com.ridelink.ridemanagement.exception.ResourceNotFoundException;
import com.ridelink.ridemanagement.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class RideService {

    // Simple flat-rate fare model: base fare + per-km rate.
    private static final double BASE_FARE = 2.50;
    private static final double PER_KM_RATE = 1.20;

    private final RideRepository rideRepository;

    public RideService(RideRepository rideRepository) {
        this.rideRepository = rideRepository;
    }

    public RideResponse requestRide(CreateRideRequest req) {
        Instant now = Instant.now();
        Ride ride = Ride.builder()
                .riderId(req.getRiderId())
                .pickupLocation(req.getPickupLocation())
                .dropoffLocation(req.getDropoffLocation())
                .pickupLat(req.getPickupLat())
                .pickupLng(req.getPickupLng())
                .dropoffLat(req.getDropoffLat())
                .dropoffLng(req.getDropoffLng())
                .distanceKm(req.getDistanceKm())
                .status(RideStatus.REQUESTED)
                .requestedAt(now)
                .updatedAt(now)
                .build();

        if (req.getDistanceKm() != null) {
            ride.setEstimatedFare(estimateFare(req.getDistanceKm()));
        }

        return RideResponse.from(rideRepository.save(ride));
    }

    public List<RideResponse> getAll() {
        return rideRepository.findAll().stream().map(RideResponse::from).toList();
    }

    public List<RideResponse> getByRider(String riderId) {
        return rideRepository.findByRiderId(riderId).stream().map(RideResponse::from).toList();
    }

    public List<RideResponse> getByDriver(String driverId) {
        return rideRepository.findByDriverId(driverId).stream().map(RideResponse::from).toList();
    }

    public List<RideResponse> getByStatus(RideStatus status) {
        return rideRepository.findByStatus(status).stream().map(RideResponse::from).toList();
    }

    public RideResponse getById(String id) {
        return RideResponse.from(findOr404(id));
    }

    public RideResponse accept(String id, AcceptRideRequest req) {
        Ride ride = findOr404(id);
        requireStatus(ride, RideStatus.REQUESTED);

        ride.setDriverId(req.getDriverId());
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setAcceptedAt(Instant.now());
        ride.setUpdatedAt(Instant.now());

        return RideResponse.from(rideRepository.save(ride));
    }

    public RideResponse start(String id) {
        Ride ride = findOr404(id);
        requireStatus(ride, RideStatus.ACCEPTED);

        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartedAt(Instant.now());
        ride.setUpdatedAt(Instant.now());

        return RideResponse.from(rideRepository.save(ride));
    }

    public RideResponse complete(String id) {
        Ride ride = findOr404(id);
        requireStatus(ride, RideStatus.IN_PROGRESS);

        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(Instant.now());
        ride.setUpdatedAt(Instant.now());
        if (ride.getDistanceKm() != null) {
            ride.setEstimatedFare(estimateFare(ride.getDistanceKm()));
        }

        return RideResponse.from(rideRepository.save(ride));
    }

    public RideResponse cancel(String id, CancelRideRequest req) {
        Ride ride = findOr404(id);

        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.CANCELLED) {
            throw new InvalidRideStateException("Cannot cancel a ride that is already " + ride.getStatus());
        }

        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancelledAt(Instant.now());
        ride.setUpdatedAt(Instant.now());
        ride.setCancellationReason(req != null ? req.getReason() : null);

        return RideResponse.from(rideRepository.save(ride));
    }

    private double estimateFare(double distanceKm) {
        return Math.round((BASE_FARE + distanceKm * PER_KM_RATE) * 100.0) / 100.0;
    }

    private void requireStatus(Ride ride, RideStatus expected) {
        if (ride.getStatus() != expected) {
            throw new InvalidRideStateException(
                    "Ride is " + ride.getStatus() + ", expected " + expected + " for this action");
        }
    }

    private Ride findOr404(String id) {
        return rideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + id));
    }
}
