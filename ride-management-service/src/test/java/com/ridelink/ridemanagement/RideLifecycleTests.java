package com.ridelink.ridemanagement;

import com.ridelink.ridemanagement.client.DriverClient;
import com.ridelink.ridemanagement.client.DriverSummary;
import com.ridelink.ridemanagement.client.FareClient;
import com.ridelink.ridemanagement.client.FareSummary;
import com.ridelink.ridemanagement.dto.*;
import com.ridelink.ridemanagement.entity.RideStatus;
import com.ridelink.ridemanagement.exception.BadRequestException;
import com.ridelink.ridemanagement.exception.InvalidRideStateException;
import com.ridelink.ridemanagement.service.RideService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RideLifecycleTests extends AbstractMongoTest {

    @Autowired
    private RideService rideService;

    @MockBean
    private DriverClient driverClient;

    @MockBean
    private FareClient fareClient;

    @BeforeEach
    void noFareServiceByDefault() {
        when(fareClient.estimate(anyDouble(), any())).thenReturn(Optional.empty());
        when(fareClient.calculateFinalFare(any(), any(), anyDouble(), any())).thenReturn(Optional.empty());
    }

    private static DriverSummary driver(String id, String status, String availability) {
        return new DriverSummary(id, "user-" + id, status, availability, "Colombo", 6.93, 79.86, 0.5);
    }

    private static String newDriverId() {
        return "drv-" + UUID.randomUUID();
    }

    private RideResponse requestRide() {
        CreateRideRequest req = new CreateRideRequest();
        req.setRiderId("rider-1");
        req.setPickupLocation("Colombo Fort");
        req.setDropoffLocation("Mount Lavinia");
        req.setPickupLat(6.9344);
        req.setPickupLng(79.8428);
        req.setDropoffLat(6.8390);
        req.setDropoffLng(79.8650);
        req.setServiceArea("Colombo");
        return rideService.requestRide(req);
    }

    private AssignDriverRequest assignReq(String driverId) {
        AssignDriverRequest req = new AssignDriverRequest();
        req.setDriverId(driverId);
        return req;
    }

    private AcceptRideRequest acceptReq(String driverId) {
        AcceptRideRequest req = new AcceptRideRequest();
        req.setDriverId(driverId);
        return req;
    }

    @Test
    void requestDerivesDistanceDurationAndFallbackFareFromCoordinates() {
        RideResponse ride = requestRide();
        assertEquals(RideStatus.REQUESTED, ride.getStatus());
        assertEquals(10.89, ride.getDistanceKm(), 0.01);          // straight-line Fort -> Mt Lavinia
        assertEquals(22, ride.getDurationMinutes());               // 10.89 km at 30 km/h
        // fallback rule: max(5.00, 2.50 + 10.89*1.20 + 22*0.20) = 19.968
        assertEquals(19.97, ride.getEstimatedFare(), 0.001);
        assertEquals(1, ride.getPassengerCount());
    }

    @Test
    void requestUsesFareServiceEstimateWhenAvailable() {
        when(fareClient.estimate(anyDouble(), any())).thenReturn(Optional.of(new FareSummary(null, 42.0, "USD")));
        assertEquals(42.0, requestRide().getEstimatedFare());
    }

    @Test
    void requestRejectsSamePickupAndDestination() {
        CreateRideRequest req = new CreateRideRequest();
        req.setRiderId("rider-1");
        req.setPickupLocation("Colombo Fort");
        req.setDropoffLocation("colombo fort ");
        assertThrows(BadRequestException.class, () -> rideService.requestRide(req));
    }

    @Test
    void fullLifecycleWithManualAssignment() {
        String driverId = newDriverId();
        when(driverClient.getDriver(driverId)).thenReturn(driver(driverId, "APPROVED", "AVAILABLE"));
        when(fareClient.calculateFinalFare(any(), eq("rider-1"), anyDouble(), any()))
                .thenReturn(Optional.of(new FareSummary("fare-123", 19.93, "USD")));

        RideResponse ride = requestRide();
        ride = rideService.assign(ride.getId(), assignReq(driverId));
        assertEquals(RideStatus.DRIVER_ASSIGNED, ride.getStatus());
        assertNotNull(ride.getAssignedAt());
        verify(driverClient).setAvailability(driverId, "ON_TRIP");

        String rideId = ride.getId();
        assertThrows(BadRequestException.class, () -> rideService.accept(rideId, acceptReq("someone-else")));

        assertEquals(RideStatus.ACCEPTED, rideService.accept(rideId, acceptReq(driverId)).getStatus());
        assertEquals(RideStatus.IN_PROGRESS, rideService.start(rideId).getStatus());

        RideResponse done = rideService.complete(rideId, null);
        assertEquals(RideStatus.COMPLETED, done.getStatus());
        assertEquals("fare-123", done.getFareId());
        assertEquals(19.93, done.getFinalFare());
        verify(driverClient).setAvailability(driverId, "AVAILABLE");
    }

    @Test
    void assignRejectsIneligibleDriver() {
        String driverId = newDriverId();
        when(driverClient.getDriver(driverId)).thenReturn(driver(driverId, "APPROVED", "OFFLINE"));
        RideResponse ride = requestRide();
        assertThrows(BadRequestException.class, () -> rideService.assign(ride.getId(), assignReq(driverId)));
    }

    @Test
    void driverCannotBeAssignedTwoRidesAtOnce() {
        String driverId = newDriverId();
        when(driverClient.getDriver(driverId)).thenReturn(driver(driverId, "APPROVED", "AVAILABLE"));
        rideService.assign(requestRide().getId(), assignReq(driverId));

        RideResponse second = requestRide();
        assertThrows(InvalidRideStateException.class, () -> rideService.assign(second.getId(), assignReq(driverId)));
    }

    @Test
    void autoAssignPicksNearestAndSkipsDriversWhoDeclined() {
        String nearest = newDriverId();
        String next = newDriverId();
        when(driverClient.findAvailable(eq("Colombo"), anyDouble(), anyDouble(), eq(1)))
                .thenReturn(List.of(driver(nearest, "APPROVED", "AVAILABLE"), driver(next, "APPROVED", "AVAILABLE")));

        RideResponse ride = requestRide();
        ride = rideService.autoAssign(ride.getId());
        assertEquals(nearest, ride.getDriverId());

        ride = rideService.decline(ride.getId(), assignReq(nearest));
        assertEquals(RideStatus.REQUESTED, ride.getStatus());
        assertNull(ride.getDriverId());

        assertEquals(next, rideService.autoAssign(ride.getId()).getDriverId());
    }

    @Test
    void autoAssignFailsWhenNoDriversAvailable() {
        when(driverClient.findAvailable(any(), any(), any(), any())).thenReturn(List.of());
        RideResponse ride = requestRide();
        assertThrows(InvalidRideStateException.class, () -> rideService.autoAssign(ride.getId()));
    }

    @Test
    void driverCanAcceptOpenRideDirectlyIfEligible() {
        String driverId = newDriverId();
        when(driverClient.getDriver(driverId)).thenReturn(driver(driverId, "APPROVED", "AVAILABLE"));
        RideResponse ride = rideService.accept(requestRide().getId(), acceptReq(driverId));
        assertEquals(RideStatus.ACCEPTED, ride.getStatus());
        assertEquals(driverId, ride.getDriverId());
    }

    @Test
    void completeUsesActualDistanceAndCancelReleasesDriver() {
        String driverId = newDriverId();
        when(driverClient.getDriver(driverId)).thenReturn(driver(driverId, "APPROVED", "AVAILABLE"));

        RideResponse ride = rideService.accept(requestRide().getId(), acceptReq(driverId));
        rideService.start(ride.getId());
        CompleteRideRequest actual = new CompleteRideRequest();
        actual.setActualDistanceKm(12.5);
        actual.setActualDurationMinutes(35);
        RideResponse done = rideService.complete(ride.getId(), actual);
        assertEquals(12.5, done.getDistanceKm());
        assertEquals(35, done.getDurationMinutes());
        verify(fareClient).calculateFinalFare(ride.getId(), "rider-1", 12.5, 35);

        String other = newDriverId();
        when(driverClient.getDriver(other)).thenReturn(driver(other, "APPROVED", "AVAILABLE"));
        RideResponse toCancel = rideService.assign(requestRide().getId(), assignReq(other));
        assertEquals(RideStatus.CANCELLED, rideService.cancel(toCancel.getId(), null).getStatus());
        verify(driverClient).setAvailability(other, "AVAILABLE");
    }
}
