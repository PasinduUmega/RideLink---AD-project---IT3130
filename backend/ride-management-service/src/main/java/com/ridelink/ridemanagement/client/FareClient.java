package com.ridelink.ridemanagement.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP client for fare-payment-service. All calls are best effort: fare-payment-service owns the
 * fare rule, and if it is down the ride flow continues (the fare can be calculated later).
 */
@Component
public class FareClient {

    private static final Logger log = LoggerFactory.getLogger(FareClient.class);

    private final RestClient restClient;

    public FareClient(RestClient.Builder builder,
                      @Value("${services.fare-payment.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public Optional<FareSummary> estimate(double distanceKm, Integer durationMinutes) {
        Map<String, Object> body = new HashMap<>();
        body.put("distanceKm", distanceKm);
        if (durationMinutes != null) body.put("durationMinutes", durationMinutes);
        return post("/api/fares/estimate", body);
    }

    public Optional<FareSummary> calculateFinalFare(String rideId, String riderId,
                                                    double distanceKm, Integer durationMinutes) {
        Map<String, Object> body = new HashMap<>();
        body.put("rideId", rideId);
        body.put("riderId", riderId);
        body.put("distanceKm", distanceKm);
        if (durationMinutes != null) body.put("durationMinutes", durationMinutes);
        return post("/api/fares", body);
    }

    private Optional<FareSummary> post(String path, Map<String, Object> body) {
        try {
            return Optional.ofNullable(restClient.post().uri(path)
                    .headers(BearerTokenForwarder::apply)
                    .body(body)
                    .retrieve()
                    .body(FareSummary.class));
        } catch (RestClientException e) {
            log.warn("fare-payment-service call {} failed: {}", path, e.getMessage());
            return Optional.empty();
        }
    }
}
