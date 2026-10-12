package com.ridelink.account.dto;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    private String fullName;

    @Email
    private String email;

    private String phone;
}
