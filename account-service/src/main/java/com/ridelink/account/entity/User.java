package com.ridelink.account.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private String id;

    @NotBlank
    private String fullName;

    @Email
    @NotBlank
    @Indexed(unique = true)
    private String email;

    @NotBlank
    private String password;

    private String phone;

    @Builder.Default
    private Role role = Role.RIDER;

    /** Kept in sync with {@link #status} for backwards compatibility (true only when ACTIVE). */
    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    private String statusReason;

    private Instant createdAt;

    private Instant updatedAt;

    /** Status with a fallback for documents stored before the status field existed. */
    public AccountStatus getStatus() {
        if (status != null) return status;
        return active ? AccountStatus.ACTIVE : AccountStatus.DEACTIVATED;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
        this.active = status == AccountStatus.ACTIVE;
    }
}
