package com.ridelink.ridemanagement.client;

/** Subset of fare-payment-service's FareResponse / FareEstimateResponse. */
public record FareSummary(String id, Double totalFare, String currency) {
}
