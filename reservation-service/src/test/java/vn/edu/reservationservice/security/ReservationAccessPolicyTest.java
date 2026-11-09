package vn.edu.reservationservice.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.reservationservice.reservation.Reservation;
import vn.edu.reservationservice.reservation.ReservationRequest;
import vn.edu.reservationservice.reservation.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationAccessPolicyTest {

    private final ReservationAccessPolicy policy = new ReservationAccessPolicy();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void studentCreate_overwritesSpoofedBodyIdentityWithJwtIdentity() {
        authenticate("student01", "STUDENT-001", "Student One", "STUDENT");

        ReservationRequest normalized = policy.enforceCreateOwnership(requestFor("STUDENT-002", "Spoofed name"));

        assertEquals("STUDENT-001", normalized.requesterId());
        assertEquals("Student One", normalized.requesterName());
        assertEquals(2L, normalized.roomId());
        assertEquals("KT1 ownership check", normalized.purpose());
    }

    @Test
    void studentRead_ignoresForeignRequesterIdFilter() {
        authenticate("student01", "STUDENT-001", "Student One", "STUDENT");

        assertEquals("STUDENT-001", policy.effectiveRequesterIdForRead("STUDENT-002"));
    }

    @Test
    void adminCreate_keepsExplicitRequestedOwner() {
        authenticate("admin01", null, "System Administrator", "ADMIN");

        ReservationRequest unchanged = policy.enforceCreateOwnership(requestFor("STUDENT-002", "Student Two"));

        assertEquals("STUDENT-002", unchanged.requesterId());
        assertEquals("Student Two", unchanged.requesterName());
    }

    @Test
    void roomManagerCreate_isDenied() {
        authenticate("roommanager01", null, "Room Manager", "ROOM_MANAGER");

        assertThrows(AccessDeniedException.class,
                () -> policy.enforceCreateOwnership(requestFor("STUDENT-002", "Student Two")));
    }

    @Test
    void studentCancel_isAllowedOnlyForOwnReservation() {
        authenticate("student01", "STUDENT-001", "Student One", "STUDENT");

        policy.assertCanCancel(reservationFor("STUDENT-001"));

        assertThrows(AccessDeniedException.class,
                () -> policy.assertCanCancel(reservationFor("STUDENT-002")));
    }

    @Test
    void adminCancel_isAllowedForAnyReservation() {
        authenticate("admin01", null, "System Administrator", "ADMIN");

        policy.assertCanCancel(reservationFor("STUDENT-002"));
    }

    private Reservation reservationFor(String requesterId) {
        return new Reservation(
                99L,
                2L,
                "A301",
                requesterId,
                "Test Student",
                "Cancellation policy test",
                LocalDateTime.of(2026, 12, 10, 9, 0),
                LocalDateTime.of(2026, 12, 10, 10, 0),
                20,
                ReservationStatus.CONFIRMED
        );
    }

    private void authenticate(String username, String requesterId, String displayName, String role) {
        AuthenticatedUser user = new AuthenticatedUser(
                username,
                requesterId,
                displayName,
                Set.of(role)
        );
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user,
                "test-jwt",
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private ReservationRequest requestFor(String requesterId, String requesterName) {
        return new ReservationRequest(
                2L,
                requesterId,
                requesterName,
                "KT1 ownership check",
                LocalDateTime.of(2026, 12, 10, 9, 0),
                LocalDateTime.of(2026, 12, 10, 10, 0),
                20
        );
    }
}
