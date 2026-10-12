package com.ridelink.drivervehicle.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    private String id;

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

    @Builder.Default
    private Integer capacity = 4;

    @Builder.Default
    private boolean active = true;

    private Instant createdAt;

    private Instant updatedAt;
}
