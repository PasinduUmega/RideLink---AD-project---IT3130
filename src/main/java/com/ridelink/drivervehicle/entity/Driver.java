package com.ridelink.drivervehicle.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Document(collection = "drivers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {

    @Id
    private String id;

    /** References the account-service User.id (MongoDB ObjectId) - no FK across service DBs. */
    @Indexed(unique = true)
    private String userId;

    @NotBlank
    @Indexed(unique = true)
    private String licenseNumber;

    private LocalDate licenseExpiry;

    @Builder.Default
    private DriverStatus status = DriverStatus.PENDING;

    @Builder.Default
    private Double rating = 5.0;

    private Instant createdAt;

    private Instant updatedAt;
}
