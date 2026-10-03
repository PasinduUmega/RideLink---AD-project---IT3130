package com.ridelink.drivervehicle.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** MongoDB vehicle document; each vehicle refers to its owner through driverId. */
@Document(collection = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    private String id;

    // Index this reference to support vehicle lookups by driver.
    @Indexed
    private String driverId;

    @NotBlank
    private String make;

    @NotBlank
    private String model;

    private Integer year;

    @NotBlank
    @Indexed(unique = true)
    private String plateNumber;

    private String color;

    // Defaults also apply when instances are created through Lombok's builder.
    @Builder.Default
    private Integer capacity = 4;

    @Builder.Default
    private boolean active = true;

    private Instant createdAt;

    private Instant updatedAt;
}
