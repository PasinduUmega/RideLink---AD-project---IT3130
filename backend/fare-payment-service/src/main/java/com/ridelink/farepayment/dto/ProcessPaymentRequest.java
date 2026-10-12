package com.ridelink.farepayment.dto;

import com.ridelink.farepayment.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessPaymentRequest {

    @NotBlank
    private String fareId;

    @NotBlank
    private String riderId;

    @NotNull
    private PaymentMethod method;

    /** Demo hook for the simulated gateway: true forces a declined (FAILED) payment. */
    private boolean simulateFailure;
}
