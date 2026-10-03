package com.ridelink.account;

import com.ridelink.account.dto.*;
import com.ridelink.account.entity.AccountStatus;
import com.ridelink.account.entity.Role;
import com.ridelink.account.exception.BadRequestException;
import com.ridelink.account.exception.UnauthorizedException;
import com.ridelink.account.service.AuthService;
import com.ridelink.account.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTests extends AbstractMongoTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    private AuthResponse register(Role role) {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Test " + role);
        req.setEmail(UUID.randomUUID() + "@example.com");
        req.setPassword("password123");
        req.setRole(role);
        return authService.register(req);
    }

    private LoginRequest login(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    @Test
    void newAccountsAreActive() {
        UserResponse user = register(Role.RIDER).getUser();
        assertEquals(AccountStatus.ACTIVE, user.getStatus());
        assertTrue(user.isActive());
    }

    @Test
    void suspendedAccountCannotLoginUntilReactivated() {
        UserResponse user = register(Role.DRIVER).getUser();

        UserResponse suspended = userService.changeStatus(user.getId(), AccountStatus.SUSPENDED, "Complaint review");
        assertEquals(AccountStatus.SUSPENDED, suspended.getStatus());
        assertEquals("Complaint review", suspended.getStatusReason());
        assertFalse(suspended.isActive());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.login(login(user.getEmail(), "password123")));
        assertEquals("Account is suspended", ex.getMessage());

        UserResponse reactivated = userService.activate(user.getId());
        assertEquals(AccountStatus.ACTIVE, reactivated.getStatus());
        assertNull(reactivated.getStatusReason());
        assertNotNull(authService.login(login(user.getEmail(), "password123")).getToken());
    }

    @Test
    void roleCanBeChangedButAdminCannotDemoteSelf() {
        UserResponse admin = register(Role.ADMIN).getUser();
        UserResponse rider = register(Role.RIDER).getUser();

        assertEquals(Role.DRIVER, userService.changeRole(rider.getId(), Role.DRIVER, admin.getId()).getRole());
        assertThrows(BadRequestException.class,
                () -> userService.changeRole(admin.getId(), Role.RIDER, admin.getId()));
    }

    @Test
    void searchFiltersByRoleAndStatus() {
        UserResponse driver = register(Role.DRIVER).getUser();
        userService.deactivate(driver.getId());

        assertTrue(userService.search(Role.DRIVER, AccountStatus.DEACTIVATED).stream()
                .anyMatch(u -> u.getId().equals(driver.getId())));
        assertTrue(userService.search(Role.RIDER, null).stream()
                .noneMatch(u -> u.getId().equals(driver.getId())));
    }

    @Test
    void changePasswordRequiresCurrentPassword() {
        UserResponse user = register(Role.RIDER).getUser();

        ChangePasswordRequest wrong = new ChangePasswordRequest();
        wrong.setCurrentPassword("not-my-password");
        wrong.setNewPassword("newPassword456");
        assertThrows(UnauthorizedException.class, () -> userService.changePassword(user.getId(), wrong));

        ChangePasswordRequest ok = new ChangePasswordRequest();
        ok.setCurrentPassword("password123");
        ok.setNewPassword("newPassword456");
        userService.changePassword(user.getId(), ok);

        assertNotNull(authService.login(login(user.getEmail(), "newPassword456")).getToken());
    }
}
