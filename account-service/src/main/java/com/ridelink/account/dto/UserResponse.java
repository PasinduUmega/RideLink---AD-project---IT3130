package com.ridelink.account.dto;

import com.ridelink.account.entity.Role;
import com.ridelink.account.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class UserResponse {
    private String id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private boolean active;
    private Instant createdAt;

    public static UserResponse from(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .role(u.getRole())
                .active(u.isActive())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
