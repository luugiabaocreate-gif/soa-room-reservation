package vn.edu.authservice.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_users")
public class AuthUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AppRole role;

    @Column(name = "requester_id", length = 100)
    private String requesterId;

    @Column(nullable = false)
    private boolean active;

    protected AuthUserEntity() {
    }

    public AuthUserEntity(
            String username,
            String passwordHash,
            String displayName,
            AppRole role,
            String requesterId,
            boolean active
    ) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.requesterId = requesterId;
        this.active = active;
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public AppRole getRole() { return role; }
    public String getRequesterId() { return requesterId; }
    public boolean isActive() { return active; }
}
