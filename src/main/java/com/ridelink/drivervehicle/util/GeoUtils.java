package com.ridelink.drivervehicle.util;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Geo helpers for the simulated location feature. There is no real GPS feed - drivers either
 * push coordinates manually or ask the service to "drift" them randomly around their position.
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /** Approximate centre points of supported service areas, used to seed simulated locations. */
    private static final Map<String, double[]> SERVICE_AREA_CENTRES = Map.of(
            "COLOMBO", new double[]{6.9271, 79.8612},
            "KANDY", new double[]{7.2906, 80.6337},
            "GALLE", new double[]{6.0535, 80.2210},
            "NEGOMBO", new double[]{7.2083, 79.8358},
            "JAFFNA", new double[]{9.6615, 80.0255},
            "KURUNEGALA", new double[]{7.4863, 80.3647},
            "MATARA", new double[]{5.9549, 80.5550}
    );

    private GeoUtils() {
    }

    /** Great-circle distance between two points using the haversine formula. */
    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        // Convert degree differences to radians for trigonometric distance calculations.
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** Finds a known city centre; an unknown or missing area returns an empty result. */
    public static Optional<double[]> centreOf(String serviceArea) {
        if (serviceArea == null) return Optional.empty();
        return Optional.ofNullable(SERVICE_AREA_CENTRES.get(serviceArea.trim().toUpperCase()));
    }

    /** Returns a random point within {@code maxKm} of the origin. */
    public static double[] randomNearby(double lat, double lng, double maxKm) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        // The square root spreads points uniformly over the disk's area; bearing picks a direction.
        double distance = maxKm * Math.sqrt(rnd.nextDouble());
        double bearing = rnd.nextDouble() * 2 * Math.PI;
        // Approximate a small movement in coordinates, correcting longitude for latitude.
        double dLat = (distance / EARTH_RADIUS_KM) * Math.cos(bearing);
        double dLng = (distance / EARTH_RADIUS_KM) * Math.sin(bearing) / Math.cos(Math.toRadians(lat));
        return new double[]{round(lat + Math.toDegrees(dLat)), round(lng + Math.toDegrees(dLng))};
    }

    /** Limits returned coordinates and distances to six decimal places. */
    public static double round(double value) {
        return Math.round(value * 1_000_000.0) / 1_000_000.0;
    }
}
