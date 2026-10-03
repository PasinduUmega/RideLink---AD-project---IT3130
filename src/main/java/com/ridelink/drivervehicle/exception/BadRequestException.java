package com.ridelink.drivervehicle.exception;

/** Signals a rejected business operation; the global handler returns HTTP 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
