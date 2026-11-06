package vn.edu.authservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import vn.edu.authservice.auth.AppRole;
import vn.edu.authservice.auth.AuthUserEntity;
import vn.edu.authservice.auth.TokenResponse;

import javax.crypto.SecretKey;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtTokenServiceTest {

    private static final String SECRET =
            "dGg1LXRlc3Qta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtMzItYnl0ZXM=";
    private static final String ISSUER = "soa-room-reservation-auth";

    private final JwtTokenService tokenService = new JwtTokenService(SECRET, ISSUER, 900);

    @Test
    void issueUserToken_containsIdentityRoleAndAllowedAudiences() {
        AuthUserEntity student = new AuthUserEntity(
                "student01",
                "not-a-real-hash-in-unit-test",
                "Student One",
                AppRole.STUDENT,
                "STUDENT-001",
                true
        );

        TokenResponse response = tokenService.issueUserToken(student);
        Claims claims = parse(response.accessToken());

        assertEquals("Bearer", response.tokenType());
        assertEquals(900, response.expiresIn());
        assertEquals("student01", claims.getSubject());
        assertEquals("Student One", claims.get("displayName", String.class));
        assertEquals("STUDENT-001", claims.get("requesterId", String.class));
        assertEquals(List.of("STUDENT"), claims.get("roles", List.class));
        assertEquals(List.of("room-service", "reservation-service"), claims.get("audiences", List.class));
        assertFalse(claims.getExpiration().before(claims.getIssuedAt()));
    }

    @Test
    void issueReservationServiceToken_isMachineIdentityForRoomServiceOnly() {
        TokenResponse response = tokenService.issueReservationServiceToken();
        Claims claims = parse(response.accessToken());

        assertEquals("reservation-service", claims.getSubject());
        assertEquals("Reservation Service", claims.get("displayName", String.class));
        assertEquals(List.of("SERVICE_RESERVATION"), claims.get("roles", List.class));
        assertEquals(List.of("room-service"), claims.get("audiences", List.class));
        assertNull(claims.get("requesterId"));
    }

    private Claims parse(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
