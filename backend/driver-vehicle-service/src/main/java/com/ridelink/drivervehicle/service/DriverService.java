package com.ridelink.drivervehicle.service;

import com.ridelink.drivervehicle.dto.*;
import com.ridelink.drivervehicle.entity.AvailabilityStatus;
import com.ridelink.drivervehicle.entity.Driver;
import com.ridelink.drivervehicle.entity.DriverStatus;
import com.ridelink.drivervehicle.entity.Vehicle;
import com.ridelink.drivervehicle.exception.BadRequestException;
import com.ridelink.drivervehicle.exception.ResourceNotFoundException;
import com.ridelink.drivervehicle.repository.DriverRepository;
import com.ridelink.drivervehicle.repository.VehicleRepository;
import com.ridelink.drivervehicle.util.GeoUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class DriverService {

    /** Search radius used by the available-drivers lookup when the caller does not pass one. */
    public static final double DEFAULT_SEARCH_RADIUS_KM = 10.0;

    /** Maximum distance a single simulated location update moves the driver. */
    private static final double SIMULATION_STEP_KM = 1.0;

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    public DriverService(DriverRepository driverRepository, VehicleRepository vehicleRepository) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public DriverResponse create(CreateDriverRequest req) {
        if (driverRepository.existsByLicenseNumber(req.getLicenseNumber())) {
            throw new BadRequestException("A driver with this license number already exists");
        }
        driverRepository.findByUserId(req.getUserId()).ifPresent(d -> {
            throw new BadRequestException("This user already has a driver profile");
        });

        Instant now = Instant.now();
        Driver driver = Driver.builder()
                .userId(req.getUserId())
                .licenseNumber(req.getLicenseNumber())
                .licenseExpiry(req.getLicenseExpiry())
                .serviceArea(normaliseArea(req.getServiceArea()))
                .status(DriverStatus.PENDING)
                .availability(AvailabilityStatus.OFFLINE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toResponse(driverRepository.save(driver));
    }

    public List<DriverResponse> getAll() {
        return driverRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<DriverResponse> getByStatus(DriverStatus status) {
        return driverRepository.findByStatus(status).stream().map(this::toResponse).toList();
    }

    public DriverResponse getById(String id) {
        return toResponse(findOr404(id));
    }

    public DriverResponse getByUserId(String userId) {
        return driverRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No driver profile for userId: " + userId));
    }

    public DriverResponse update(String id, UpdateDriverRequest req) {
        Driver driver = findOr404(id);
        if (req.getLicenseNumber() != null) driver.setLicenseNumber(req.getLicenseNumber());
        if (req.getLicenseExpiry() != null) driver.setLicenseExpiry(req.getLicenseExpiry());
        if (req.getServiceArea() != null) driver.setServiceArea(normaliseArea(req.getServiceArea()));
        driver.setUpdatedAt(Instant.now());
        return toResponse(driverRepository.save(driver));
    }

    public DriverResponse updateStatus(String id, UpdateDriverStatusRequest req) {
        Driver driver = findOr404(id);
        driver.setStatus(req.getStatus());
        // A driver who is no longer approved must drop off the matching pool immediately.
        if (req.getStatus() != DriverStatus.APPROVED) {
            driver.setAvailability(AvailabilityStatus.OFFLINE);
        }
        driver.setUpdatedAt(Instant.now());
        return toResponse(driverRepository.save(driver));
    }

    /**
     * Going AVAILABLE requires an APPROVED driver with at least one active vehicle.
     * OFFLINE and ON_TRIP can always be set (ON_TRIP is normally set by ride-management-service).
     */
    public DriverResponse updateAvailability(String id, UpdateAvailabilityRequest req) {
        Driver driver = findOr404(id);

        if (req.getAvailability() == AvailabilityStatus.AVAILABLE) {
            if (driver.getStatus() != DriverStatus.APPROVED) {
                throw new BadRequestException("Only APPROVED drivers can go AVAILABLE (current status: "
                        + driver.getStatus() + ")");
            }
            if (activeVehicles(driver).isEmpty()) {
                throw new BadRequestException("Driver needs at least one active vehicle to go AVAILABLE");
            }
        }

        driver.setAvailability(req.getAvailability());
        driver.setUpdatedAt(Instant.now());
        return toResponse(driverRepository.save(driver));
    }

    public DriverResponse updateLocation(String id, UpdateLocationRequest req) {
        Driver driver = findOr404(id);
        return toResponse(saveLocation(driver, req.getLat(), req.getLng()));
    }

    /**
     * Simulates GPS movement: moves the driver to a random point within 1 km of their current
     * location, or places them near their service-area centre if they have no location yet.
     */
    public DriverResponse simulateLocation(String id) {
        Driver driver = findOr404(id);

        double[] origin;
        if (driver.getCurrentLat() != null && driver.getCurrentLng() != null) {
            origin = new double[]{driver.getCurrentLat(), driver.getCurrentLng()};
        } else {
            origin = GeoUtils.centreOf(driver.getServiceArea()).orElseThrow(() -> new BadRequestException(
                    "Cannot simulate a location: driver has no current location and service area '"
                            + driver.getServiceArea() + "' has no known centre. Set a location manually first."));
        }

        double[] next = GeoUtils.randomNearby(origin[0], origin[1], SIMULATION_STEP_KM);
        return toResponse(saveLocation(driver, next[0], next[1]));
    }

    /**
     * Eligible drivers for a new ride: APPROVED + AVAILABLE, owning an active vehicle with enough
     * seats, optionally restricted to a service area and/or a radius around the pickup point.
     * When a pickup point is given, results are sorted nearest-first.
     */
    public List<DriverResponse> findAvailable(String serviceArea, Double lat, Double lng,
                                              Double radiusKm, Integer minCapacity) {
        if ((lat == null) != (lng == null)) {
            throw new BadRequestException("Provide both lat and lng, or neither");
        }
        boolean byDistance = lat != null;
        double radius = radiusKm != null ? radiusKm : DEFAULT_SEARCH_RADIUS_KM;
        int seats = minCapacity != null ? minCapacity : 1;
        String area = normaliseArea(serviceArea);

        return driverRepository.findByStatusAndAvailability(DriverStatus.APPROVED, AvailabilityStatus.AVAILABLE)
                .stream()
                .filter(d -> area == null || area.equalsIgnoreCase(d.getServiceArea()))
                .filter(d -> activeVehicles(d).stream()
                        .anyMatch(v -> v.getCapacity() == null || v.getCapacity() >= seats))
                .map(d -> {
                    Double distance = null;
                    if (byDistance) {
                        if (d.getCurrentLat() == null || d.getCurrentLng() == null) return null;
                        distance = GeoUtils.round(GeoUtils.distanceKm(lat, lng, d.getCurrentLat(), d.getCurrentLng()));
                        if (distance > radius) return null;
                    }
                    return DriverResponse.from(d, vehicleResponses(d), distance);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(DriverResponse::getDistanceKm,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public void delete(String id) {
        Driver driver = findOr404(id);
        vehicleRepository.findByDriverId(driver.getId()).forEach(vehicleRepository::delete);
        driverRepository.delete(driver);
    }

    private Driver saveLocation(Driver driver, double lat, double lng) {
        driver.setCurrentLat(lat);
        driver.setCurrentLng(lng);
        driver.setLocationUpdatedAt(Instant.now());
        driver.setUpdatedAt(Instant.now());
        return driverRepository.save(driver);
    }

    private List<Vehicle> activeVehicles(Driver driver) {
        return vehicleRepository.findByDriverId(driver.getId()).stream().filter(Vehicle::isActive).toList();
    }

    private List<VehicleResponse> vehicleResponses(Driver driver) {
        return vehicleRepository.findByDriverId(driver.getId()).stream().map(VehicleResponse::from).toList();
    }

    private DriverResponse toResponse(Driver driver) {
        return DriverResponse.from(driver, vehicleResponses(driver));
    }

    private static String normaliseArea(String area) {
        return area == null || area.isBlank() ? null : area.trim();
    }

    private Driver findOr404(String id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
    }
}
