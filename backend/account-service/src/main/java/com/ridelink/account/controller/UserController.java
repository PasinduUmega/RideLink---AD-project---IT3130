package com.ridelink.account.controller;

import com.ridelink.account.dto.*;
import com.ridelink.account.entity.AccountStatus;
import com.ridelink.account.entity.Role;
import com.ridelink.account.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ---- Own profile (user id is taken from the JWT subject) ----

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(userService.getById(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(@AuthenticationPrincipal String userId,
                                                        @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(userId, request));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changeMyPassword(@AuthenticationPrincipal String userId,
                                                 @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    // ---- User administration ----

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll(@RequestParam(required = false) Role role,
                                                     @RequestParam(required = false) AccountStatus status) {
        if (role != null || status != null) {
            return ResponseEntity.ok(userService.search(role, status));
        }
        return ResponseEntity.ok(userService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable String id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivate(@PathVariable String id) {
        return ResponseEntity.ok(userService.deactivate(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserResponse> activate(@PathVariable String id) {
        return ResponseEntity.ok(userService.activate(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> changeStatus(@PathVariable String id,
                                                     @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(userService.changeStatus(id, request.getStatus(), request.getReason()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeRole(@PathVariable String id,
                                                   @AuthenticationPrincipal String adminId,
                                                   @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(userService.changeRole(id, request.getRole(), adminId));
    }
}
