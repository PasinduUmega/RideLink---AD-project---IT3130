package com.ridelink.farepayment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The single, documented RideLink fare rule - used for both estimates and final fares:
 *
 * <pre>
 *   durationMinutes = given value, or round(distanceKm / averageSpeedKmh * 60) if omitted
 *   distanceFare    = distanceKm      x perKmRate
 *   timeFare        = durationMinutes x perMinuteRate
 *   subtotal        = baseFare + distanceFare + timeFare
 *   totalFare       = max(minimumFare, subtotal)          (every amount rounded to 2 dp)
 * </pre>
 *
 * Defaults: baseFare 2.50, perKmRate 1.20, perMinuteRate 0.20, minimumFare 5.00,
 * averageSpeedKmh 30, currency USD - all overridable via application.yml / env vars.
 */
@Component
public class FareCalculator {

    public static final String RULE =
            "totalFare = max(minimumFare, baseFare + distanceKm x perKmRate + durationMinutes x perMinuteRate)";

    private final double baseFare;
    private final double perKmRate;
    private final double perMinuteRate;
    private final double minimumFare;
    private final double averageSpeedKmh;
    private final String currency;

    public FareCalculator(@Value("${fare.base-fare}") double baseFare,
                          @Value("${fare.per-km-rate}") double perKmRate,
                          @Value("${fare.per-minute-rate:0.20}") double perMinuteRate,
                          @Value("${fare.minimum-fare:5.00}") double minimumFare,
                          @Value("${fare.average-speed-kmh:30}") double averageSpeedKmh,
                          @Value("${fare.currency}") String currency) {
        this.baseFare = baseFare;
        this.perKmRate = perKmRate;
        this.perMinuteRate = perMinuteRate;
        this.minimumFare = minimumFare;
        this.averageSpeedKmh = averageSpeedKmh;
        this.currency = currency;
    }

    public Breakdown calculate(double distanceKm, Integer durationMinutes) {
        int minutes = durationMinutes != null ? durationMinutes : estimateDurationMinutes(distanceKm);
        double distanceFare = round(distanceKm * perKmRate);
        double timeFare = round(minutes * perMinuteRate);
        double subtotal = round(baseFare + distanceFare + timeFare);
        boolean minimumApplied = subtotal < minimumFare;
        double total = minimumApplied ? minimumFare : subtotal;
        return new Breakdown(distanceKm, minutes, durationMinutes == null, baseFare, distanceFare, timeFare,
                subtotal, minimumFare, minimumApplied, round(total), currency);
    }

    public int estimateDurationMinutes(double distanceKm) {
        return (int) Math.round(distanceKm / averageSpeedKmh * 60);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record Breakdown(
            double distanceKm,
            int durationMinutes,
            boolean durationEstimated,
            double baseFare,
            double distanceFare,
            double timeFare,
            double subtotal,
            double minimumFare,
            boolean minimumFareApplied,
            double totalFare,
            String currency) {
    }
}
