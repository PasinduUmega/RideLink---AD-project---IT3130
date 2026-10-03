package com.ridelink.ridemanagement.service;

import com.ridelink.ridemanagement.client.DriverClient;
import com.ridelink.ridemanagement.client.DriverSummary;
import com.ridelink.ridemanagement.client.FareClient;
import com.ridelink.ridemanagement.dto.*;
import com.ridelink.ridemanagement.entity.Ride;
import com.ridelink.ridemanagement.entity.RideStatus;
import com.ridelink.ridemanagement.exception.BadRequestException;
import com.ridelink.ridemanagement.exception.InvalidRideStateException;
import com.ridelink.ridemanagement.exception.ResourceNotFoundException;
import com.ridelink.ridemanagement.repository.RideRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Ride lifecycle:
 * <pre>
 *   REQUESTED --assign--> DRIVER_ASSIGNED --accept--> ACCEPTED --start--> IN_PROGRESS --complete--> COMPLETED
 *       |                     |   \--decline--> REQUESTED
 *       \--accept (driver picks an open ride)--> ACCEPTED
 *   any non-final state --cancel--> CANCELLED
 * </pre>
 */
@Service
public class RideService {

    /**
     * Local fallback used only when fare-payment-service is unreachable. Mirrors that service's
     * documented default rule so estimates stay consistent.
     */
    static final double FALLBACK_BASE_FARE = 2.50;
    static final double FALLBACK_PER_KM_RATE = 1.20;
    static final double FALLBACK_PER_MINUTE_RATE = 0.20;
    static final double FALLBACK_MINIMUM_FARE = 5.00;

    /** Average city speed used to estimate trip duration from distance. */
    static final double AVERAGE_SPEED_KMH = 30.0;

    private static final Set<RideStatus> DRIVER_BUSY_STATES =
            EnumSet.of(RideStatus.DRIVER_ASSIGNED, RideStatus.ACCEPTED, RideStatus.IN_PROGRESS);

    private final RideRepository rideRepository;
    private final DriverClient driverClient;
    private final FareClient fareClient;

    public RideService(RideRepository rideRepository, DriverClient driverClient, FareClient fareClient) {
        this.rideRepository = rideRepository;
        this.driverClient = driverClient;
        this.fareClient = fareClient;
    }

    public RideResponse requestRide(CreateRideRequest req) {
        if (req.getPickupLocation().trim().equalsIgnoreCase(req.getDropoffLocation().trim())) {
            throw new BadRequestException("Pickup and destination must be different");
        }
        boolean hasPickupCoords = req.getPickupLat() != null && req.getPickupLng() != null;
        boolean hasDropoffCoords = req.getDropoffLat() != null && req.getDropoffLng() != null;
        if ((req.getPickupLat() == null) != (req.getPickupLng() == null)
                || (req.getDropoffLat() == null) != (req.getDropoffLng() == null)) {
            throw new BadRequestException("Coordinates must be given as lat/lng pairs");
        }

        Double distanceKm = req.getDistanceKm();
        if (distanceKm == null && hasPickupCoords && hasDropoffCoords) {
            distanceKm = round(haversineKm(req.getPickupLat(), req.getPickupLng(),
                    req.getDropoffLat(), req.getDropoffLng()));
            if (distanceKm <= 0) {
                throw new BadRequestException("Pickup and destination coordinates must be different");
            }
        }

        Instant now = Instant.now();
        Ride ride = Ride.builder()
                .riderId(req.getRiderId())
                .pickupLocation(req.getPickupLocation().trim())
                .dropoffLocation(req.getDropoffLocation().trim())
                .pickupLat(req.getPickupLat())
                .pickupLng(req.getPickupLng())
                .dropoffLat(req.getDropoffLat())
                .dropoffLng(req.getDropoffLng())
                .serviceArea(req.getServiceArea())
                .passengerCount(req.getPassengerCount() != null ? req.getPassengerCount() : 1)
                .distanceKm(distanceKm)
                .status(RideStatus.REQUESTED)
                .requestedAt(now)
                .updatedAt(now)
                .build();

        if (distanceKm != null) {
            int duration = estimateDurationMinutes(distanceKm);
            ride.setDurationMinutes(duration);
            ride.setEstimatedFare(estimateFare(distanceKm, duration));
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

    /** Dispatcher/admin assigns a specific driver. The driver must then accept or decline. */
    public RideResponse assign(String id, AssignDriverRequest req) {
        Ride ride = findOr404(id);
        requireStatus(ride, RideStatus.REQUESTED);
        DriverSummary driver = driverClient.getDriver(req.getDriverId());
        requireEligible(driver);
        return RideResponse.from(assignTo(ride, driver.id()));
    }

    /** Picks the nearest eligible driver (same service area, enough seats, not previously declined). */
    public RideResponse autoAssign(String id) {
        Ride ride = findOr404(id);
        requireStatus(ride, RideStatus.REQUESTED);

        DriverSummary chosen = driverClient
                .findAvailable(ride.getServiceArea(), ride.getPickupLat(), ride.getPickupLng(), ride.getPassengerCount())
                .stream()
                .filter(d -> !ride.getDeclinedDriverIds().contains(d.id()))
                .filter(d -> !hasActiveRide(d.id()))
                .findFirst()
                .orElseThrow(() -> new InvalidRideStateException("No eligible drivers are available for this ride"));

        return RideResponse.from(assignTo(ride, chosen.id()));
    }

    /** The assigned driver turns the ride down; it goes back to the pool. */
    public RideResponse decline(String id, AssignDriverRequest req) {
        Ride ride = findOr404(id);
        requireStatus(ride, RideStatus.DRIVER_ASSIGNED);
        if (!ride.getDriverId().equals(req.getDriverId())) {
            throw new BadRequestException("Only the assigned driver can decline this ride");
        }

        ride.getDeclinedDriverIds().add(ride.getDriverId());
        driverClient.setAvailability(ride.getDriverId(), "AVAILABLE");
        ride.setDriverId(null);
        ride.setAssignedAt(null);
        ride.setStatus(RideStatus.REQUESTED);
        ride.setUpdatedAt(Instant.now());
        return RideResponse.from(rideRepository.save(ride));
    }

    /**
     * Either the assigned driver confirms (DRIVER_ASSIGNED) or a driver picks up an open
     * ride directly (REQUESTED - the driver is checked for eligibility first).
     */
    public RideResponse accept(String id, AcceptRideRequest req) {
        Ride ride = findOr404(id);

        if (ride.getStatus() == RideStatus.DRIVER_ASSIGNED) {
            if (!ride.getDriverId().equals(req.getDriverId())) {
                throw new BadRequestException("This ride is assigned to a different driver");
            }
        } else if (ride.getStatus() == RideStatus.REQUESTED) {
            requireEligible(driverClient.getDriver(req.getDriverId()));
            ride.setDriverId(req.getDriverId());
            driverClient.setAvailability(req.getDriverId(), "ON_TRIP");
        } else {
            throw new InvalidRideStateException(
                    "Ride is " + ride.getStatus() + ", expected REQUESTED or DRIVER_ASSIGNED for this action");
        }

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

    public RideResponse complete(String id, CompleteRideRequest req) {
        Ride ride = findOr404(id);
        requireStatus(ride, RideStatus.IN_PROGRESS);

        Instant now = Instant.now();
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(now);
        ride.setUpdatedAt(now);

        if (req != null && req.getActualDistanceKm() != null) {
            ride.setDistanceKm(req.getActualDistanceKm());
        }
        if (req != null && req.getActualDurationMinutes() != null) {
            ride.setDurationMinutes(req.getActualDurationMinutes());
        } else if (ride.getStartedAt() != null) {
            long minutes = Duration.between(ride.getStartedAt(), now).toMinutes();
            // Rides completed within seconds (e.g. in demos) keep the distance-based estimate.
            if (minutes > 0) ride.setDurationMinutes((int) minutes);
        }

        ride = rideRepository.save(ride);
        driverClient.setAvailability(ride.getDriverId(), "AVAILABLE");

        if (ride.getDistanceKm() != null) {
            Ride saved = ride;
            fareClient.calculateFinalFare(ride.getId(), ride.getRiderId(), ride.getDistanceKm(), ride.getDurationMinutes())
                    .ifPresent(fare -> {
                        saved.setFareId(fare.id());
                        saved.setFinalFare(fare.totalFare());
                    });
            ride = rideRepository.save(saved);
        }

        return RideResponse.from(ride);
    }

    public RideResponse cancel(String id, CancelRideRequest req) {
        Ride ride = findOr404(id);

        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.CANCELLED) {
            throw new InvalidRideStateException("Cannot cancel a ride that is already " + ride.getStatus());
        }

        if (ride.getDriverId() != null) {
            driverClient.setAvailability(ride.getDriverId(), "AVAILABLE");
        }

        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancelledAt(Instant.now());
        ride.setUpdatedAt(Instant.now());
        ride.setCancellationReason(req != null ? req.getReason() : null);
        ride.setCancelledBy(currentRole());

        return RideResponse.from(rideRepository.save(ride));
    }

    private Ride assignTo(Ride ride, String driverId) {
        if (hasActiveRide(driverId)) {
            throw new InvalidRideStateException("Driver " + driverId + " already has an active ride");
        }
        ride.setDriverId(driverId);
        ride.setStatus(RideStatus.DRIVER_ASSIGNED);
        ride.setAssignedAt(Instant.now());
        ride.setUpdatedAt(Instant.now());
        Ride saved = rideRepository.save(ride);
        // Reserve the driver so they are not matched to another ride while deciding.
        driverClient.setAvailability(driverId, "ON_TRIP");
        return saved;
    }

    private boolean hasActiveRide(String driverId) {
        return DRIVER_BUSY_STATES.stream()
                .anyMatch(s -> !rideRepository.findByDriverIdAndStatus(driverId, s).isEmpty());
    }

    private void requireEligible(DriverSummary driver) {
        if (!driver.isApproved()) {
            throw new BadRequestException("Driver " + driver.id() + " is not approved (status: " + driver.status() + ")");
        }
        if (!driver.isAvailable()) {
            throw new BadRequestException("Driver " + driver.id() + " is not available (availability: " + driver.availability() + ")");
        }
    }

    private double estimateFare(double distanceKm, int durationMinutes) {
        return fareClient.estimate(distanceKm, durationMinutes)
                .map(f -> f.totalFare())
                .orElseGet(() -> round(Math.max(FALLBACK_MINIMUM_FARE,
                        FALLBACK_BASE_FARE + distanceKm * FALLBACK_PER_KM_RATE
                                + durationMinutes * FALLBACK_PER_MINUTE_RATE)));
    }

    static int estimateDurationMinutes(double distanceKm) {
        return (int) Math.max(1, Math.round(distanceKm / AVERAGE_SPEED_KMH * 60));
    }

    static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String currentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse(null);
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
