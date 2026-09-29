package com.ridelink.account.service;

import com.ridelink.account.dto.ChangePasswordRequest;
import com.ridelink.account.dto.UpdateUserRequest;
import com.ridelink.account.dto.UserResponse;
import com.ridelink.account.entity.AccountStatus;
import com.ridelink.account.entity.Role;
import com.ridelink.account.entity.User;
import com.ridelink.account.exception.BadRequestException;
import com.ridelink.account.exception.ResourceNotFoundException;
import com.ridelink.account.exception.UnauthorizedException;
import com.ridelink.account.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> getAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    public List<UserResponse> search(Role role, AccountStatus status) {
        List<User> users = role != null ? userRepository.findByRole(role) : userRepository.findAll();
        return users.stream()
                .filter(u -> status == null || u.getStatus() == status)
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse getById(String id) {
        return UserResponse.from(findUserOr404(id));
    }

    public UserResponse update(String id, UpdateUserRequest req) {
        User user = findUserOr404(id);

        if (req.getEmail() != null && !req.getEmail().equals(user.getEmail())
                && userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getEmail() != null) user.setEmail(req.getEmail());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        user.setUpdatedAt(Instant.now());

        return UserResponse.from(userRepository.save(user));
    }

    public void changePassword(String id, ChangePasswordRequest req) {
        User user = findUserOr404(id);
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        if (req.getCurrentPassword().equals(req.getNewPassword())) {
            throw new BadRequestException("New password must be different from the current password");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    public void delete(String id) {
        User user = findUserOr404(id);
        userRepository.delete(user);
    }

    public UserResponse deactivate(String id) {
        return changeStatus(id, AccountStatus.DEACTIVATED, null);
    }

    public UserResponse activate(String id) {
        return changeStatus(id, AccountStatus.ACTIVE, null);
    }

    public UserResponse changeStatus(String id, AccountStatus status, String reason) {
        User user = findUserOr404(id);
        user.setStatus(status);
        user.setStatusReason(status == AccountStatus.ACTIVE ? null : reason);
        user.setUpdatedAt(Instant.now());
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse changeRole(String id, Role role, String actingUserId) {
        User user = findUserOr404(id);
        if (user.getId().equals(actingUserId) && user.getRole() == Role.ADMIN && role != Role.ADMIN) {
            throw new BadRequestException("Admins cannot remove their own ADMIN role");
        }
        user.setRole(role);
        user.setUpdatedAt(Instant.now());
        return UserResponse.from(userRepository.save(user));
    }

    private User findUserOr404(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
