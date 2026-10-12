package com.ridelink.account.dto;

import com.ridelink.account.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStatusRequest {
    @NotNull
    private AccountStatus status;

    /** Optional - recorded on the account for audit purposes. */
    private String reason;
}
