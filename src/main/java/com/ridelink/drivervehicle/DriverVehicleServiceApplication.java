package com.ridelink.drivervehicle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Starts the API server and discovers this package's controllers, services and configuration. */
@SpringBootApplication
public class DriverVehicleServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DriverVehicleServiceApplication.class, args);
    }
}
