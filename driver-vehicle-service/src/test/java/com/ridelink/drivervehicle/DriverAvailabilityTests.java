package com.ridelink.drivervehicle;

import com.ridelink.drivervehicle.dto.*;
import com.ridelink.drivervehicle.entity.AvailabilityStatus;
import com.ridelink.drivervehicle.entity.DriverStatus;
import com.ridelink.drivervehicle.exception.BadRequestException;
import com.ridelink.drivervehicle.service.DriverService;
import com.ridelink.drivervehicle.service.VehicleService;
import com.ridelink.drivervehicle.util.GeoUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DriverAvailabilityTests extends AbstractMongoTest {

    @Autowired
    private DriverService driverService;

    @Autowired
    private VehicleService vehicleService;

    private DriverResponse newDriver(String area) {
        CreateDriverRequest req = new CreateDriverRequest();
        req.setUserId(UUID.randomUUID().toString());
        req.setLicenseNumber("LIC-" + UUID.randomUUID());
        req.setServiceArea(area);
        return driverService.create(req);
    }

    private void addVehicle(String driverId, int capacity) {
        CreateVehicleRequest req = new CreateVehicleRequest();
        req.setDriverId(driverId);
        req.setMake("Toyota");
        req.setModel("Prius");
        req.setPlateNumber("CAB-" + UUID.randomUUID().toString().substring(0, 8));
        req.setCapacity(capacity);
        vehicleService.create(req);
    }

    private void approve(String driverId) {
        UpdateDriverStatusRequest req = new UpdateDriverStatusRequest();
        req.setStatus(DriverStatus.APPROVED);
        driverService.updateStatus(driverId, req);
    }

    private DriverResponse setAvailability(String driverId, AvailabilityStatus availability) {
        UpdateAvailabilityRequest req = new UpdateAvailabilityRequest();
        req.setAvailability(availability);
        return driverService.updateAvailability(driverId, req);
    }

    private void moveTo(String driverId, double lat, double lng) {
        UpdateLocationRequest req = new UpdateLocationRequest();
        req.setLat(lat);
        req.setLng(lng);
        driverService.updateLocation(driverId, req);
    }

    /** Approved, with a vehicle, located at the given point and AVAILABLE. */
    private DriverResponse readyDriver(String area, int capacity, double lat, double lng) {
        DriverResponse d = newDriver(area);
        addVehicle(d.getId(), capacity);
        approve(d.getId());
        moveTo(d.getId(), lat, lng);
        setAvailability(d.getId(), AvailabilityStatus.AVAILABLE);
        return d;
    }

    @Test
    void newDriverStartsOfflineWithServiceArea() {
        DriverResponse d = newDriver("Colombo");
        assertEquals(AvailabilityStatus.OFFLINE, d.getAvailability());
        assertEquals("Colombo", d.getServiceArea());
    }

    @Test
    void cannotGoAvailableUnlessApprovedWithVehicle() {
        DriverResponse d = newDriver("Colombo");
        assertThrows(BadRequestException.class, () -> setAvailability(d.getId(), AvailabilityStatus.AVAILABLE));

        approve(d.getId());
        assertThrows(BadRequestException.class, () -> setAvailability(d.getId(), AvailabilityStatus.AVAILABLE));

        addVehicle(d.getId(), 4);
        assertEquals(AvailabilityStatus.AVAILABLE, setAvailability(d.getId(), AvailabilityStatus.AVAILABLE).getAvailability());
    }

    @Test
    void suspendingDriverForcesOffline() {
        DriverResponse d = readyDriver("Kandy", 4, 7.29, 80.63);
        UpdateDriverStatusRequest req = new UpdateDriverStatusRequest();
        req.setStatus(DriverStatus.SUSPENDED);
        assertEquals(AvailabilityStatus.OFFLINE, driverService.updateStatus(d.getId(), req).getAvailability());
    }

    @Test
    void simulatedLocationStartsAtAreaCentreAndStaysNearby() {
        DriverResponse d = newDriver("Galle");
        DriverResponse first = driverService.simulateLocation(d.getId());
        assertNotNull(first.getCurrentLat());
        assertTrue(GeoUtils.distanceKm(6.0535, 80.2210, first.getCurrentLat(), first.getCurrentLng()) <= 1.01);

        DriverResponse second = driverService.simulateLocation(d.getId());
        assertTrue(GeoUtils.distanceKm(first.getCurrentLat(), first.getCurrentLng(),
                second.getCurrentLat(), second.getCurrentLng()) <= 1.01);
    }

    @Test
    void simulatedLocationFailsForUnknownAreaWithoutLocation() {
        DriverResponse d = newDriver("Atlantis");
        assertThrows(BadRequestException.class, () -> driverService.simulateLocation(d.getId()));
    }

    @Test
    void availableLookupFiltersByAreaCapacityRadiusAndSortsByDistance() {
        String area = "Area-" + UUID.randomUUID();
        DriverResponse near = readyDriver(area, 4, 6.9271, 79.8612);
        DriverResponse far = readyDriver(area, 4, 6.9500, 79.8612);   // ~2.5 km north
        DriverResponse small = readyDriver(area, 2, 6.9275, 79.8612);
        DriverResponse outOfRange = readyDriver(area, 4, 7.2906, 80.6337); // Kandy
        DriverResponse offline = readyDriver(area, 4, 6.9271, 79.8612);
        setAvailability(offline.getId(), AvailabilityStatus.OFFLINE);

        List<DriverResponse> result = driverService.findAvailable(area, 6.9271, 79.8612, 10.0, 3);

        assertEquals(List.of(near.getId(), far.getId()), result.stream().map(DriverResponse::getId).toList());
        assertEquals(0.0, result.get(0).getDistanceKm(), 0.01);
        assertFalse(result.stream().anyMatch(r -> r.getId().equals(small.getId())
                || r.getId().equals(outOfRange.getId()) || r.getId().equals(offline.getId())));
    }

    @Test
    void availableLookupRequiresBothCoordinates() {
        assertThrows(BadRequestException.class, () -> driverService.findAvailable(null, 6.9, null, null, null));
    }
}
