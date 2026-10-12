package com.ridelink.ridemanagement.client;

import com.ridelink.ridemanagement.exception.BadRequestException;
import com.ridelink.ridemanagement.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.util.List;
import java.util.Map;

/** HTTP client for driver-vehicle-service. */
@Component
public class DriverClient {

    private static final Logger log = LoggerFactory.getLogger(DriverClient.class);

    private final RestClient restClient;

    public DriverClient(RestClient.Builder builder,
                        @Value("${services.driver-vehicle.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public DriverSummary getDriver(String driverId) {
        try {
            return restClient.get().uri("/api/drivers/{id}", driverId)
                    .headers(BearerTokenForwarder::apply)
                    .retrieve()
                    .body(DriverSummary.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new BadRequestException("Driver not found with id: " + driverId);
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    /** Eligible drivers, nearest first when a pickup point is given. */
    public List<DriverSummary> findAvailable(String serviceArea, Double lat, Double lng, Integer minCapacity) {
        try {
            List<DriverSummary> drivers = restClient.get()
                    .uri(b -> {
                        UriBuilder ub = b.path("/api/drivers/available");
                        if (serviceArea != null) ub.queryParam("serviceArea", serviceArea);
                        if (lat != null && lng != null) ub.queryParam("lat", lat).queryParam("lng", lng);
                        if (minCapacity != null) ub.queryParam("minCapacity", minCapacity);
                        return ub.build();
                    })
                    .headers(BearerTokenForwarder::apply)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<DriverSummary>>() {});
            return drivers != null ? drivers : List.of();
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    /** Best effort - a failure here must not roll back the ride state change. */
    public void setAvailability(String driverId, String availability) {
        try {
            restClient.patch().uri("/api/drivers/{id}/availability", driverId)
                    .headers(BearerTokenForwarder::apply)
                    .body(Map.of("availability", availability))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Could not set driver {} availability to {}: {}", driverId, availability, e.getMessage());
        }
    }

    private ServiceUnavailableException unavailable(RestClientException e) {
        if (e instanceof HttpClientErrorException http && http.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return new ServiceUnavailableException("driver-vehicle-service rejected the request (missing or invalid token)");
        }
        log.warn("driver-vehicle-service call failed: {}", e.getMessage());
        return new ServiceUnavailableException("driver-vehicle-service is unavailable: " + e.getMessage());
    }
}
