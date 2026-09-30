package com.ridelink.account.dto;

import com.ridelink.account.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoleRequest {
    @NotNull
    private Role role;
}
