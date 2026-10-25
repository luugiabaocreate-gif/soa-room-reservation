package vn.edu.reservationservice.security;

import java.util.Set;

public record AuthenticatedUser(
        String username,
        String requesterId,
        String displayName,
        Set<String> roles
) {
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
