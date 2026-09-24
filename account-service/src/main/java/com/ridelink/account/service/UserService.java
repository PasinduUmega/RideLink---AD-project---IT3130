package com.ridelink.account.service;

import com.ridelink.account.dto.UpdateUserRequest;
import com.ridelink.account.dto.UserResponse;
import com.ridelink.account.entity.User;
import com.ridelink.account.exception.BadRequestException;
import com.ridelink.account.exception.ResourceNotFoundException;
import com.ridelink.account.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> getAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
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

    public void delete(String id) {
        User user = findUserOr404(id);
        userRepository.delete(user);
    }

    public UserResponse deactivate(String id) {
        User user = findUserOr404(id);
        user.setActive(false);
        user.setUpdatedAt(Instant.now());
        return UserResponse.from(userRepository.save(user));
    }

    private User findUserOr404(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
