package com.ridelink.account;

import com.ridelink.account.dto.AuthResponse;
import com.ridelink.account.dto.LoginRequest;
import com.ridelink.account.dto.RegisterRequest;
import com.ridelink.account.entity.Role;
import com.ridelink.account.exception.BadRequestException;
import com.ridelink.account.exception.UnauthorizedException;
import com.ridelink.account.security.JwtUtil;
import com.ridelink.account.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AccountServiceApplicationTests {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthService authService;

    @Test
    void contextLoads() {
    }

    @Test
    void testJwtTokenGenerationAndParsing() {
        String token = jwtUtil.generateToken("665f1a2b3c4d5e6f7a8b9c0d", "test@example.com", "RIDER");
        assertNotNull(token);
        assertTrue(jwtUtil.isValid(token));
        assertEquals("665f1a2b3c4d5e6f7a8b9c0d", jwtUtil.getUserId(token));
        assertEquals("RIDER", jwtUtil.getRole(token));
    }

    @Test
    void testAuthServiceRegisterAndLogin() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFullName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setPhone("+1234567890");
        registerRequest.setRole(Role.RIDER);

        AuthResponse registerResponse = authService.register(registerRequest);
        assertNotNull(registerResponse);
        assertNotNull(registerResponse.getToken());
        assertEquals("Bearer", registerResponse.getTokenType());
        assertEquals("john@example.com", registerResponse.getUser().getEmail());

        // Duplicate registration should fail
        assertThrows(BadRequestException.class, () -> authService.register(registerRequest));

        // Successful Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("password123");

        AuthResponse loginResponse = authService.login(loginRequest);
        assertNotNull(loginResponse);
        assertNotNull(loginResponse.getToken());
        assertEquals("john@example.com", loginResponse.getUser().getEmail());

        // Login with wrong password
        loginRequest.setPassword("wrongpassword");
        assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest));
    }
}
