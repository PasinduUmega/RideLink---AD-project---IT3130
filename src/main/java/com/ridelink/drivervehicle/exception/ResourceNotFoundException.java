package com.ridelink.drivervehicle.exception;

/** Signals a missing driver or vehicle; the global handler returns HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
