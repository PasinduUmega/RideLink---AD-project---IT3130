package com.ridelink.ridemanagement.exception;

/** A downstream RideLink service could not be reached. */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
