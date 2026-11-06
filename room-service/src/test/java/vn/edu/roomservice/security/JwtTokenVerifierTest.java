package vn.edu.roomservice.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenVerifierTest {

    private static final String SECRET =
            "dGg1LXRlc3Qta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtMzItYnl0ZXM=";
    private static final String ISSUER = "soa-room-reservation-auth";

    private final JwtTokenVerifier verifier = new JwtTokenVerifier(SECRET, ISSUER, "room-service");

    @Test
    void authenticate_acceptsSignedRoomAudienceTokenAndMapsRole() {
        UsernamePasswordAuthenticationToken authentication = verifier.authenticate(
                tokenFor("student01", "STUDENT-001", List.of("STUDENT"), List.of("room-service"))
        );

        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        assertEquals("student01", user.username());
        assertEquals("STUDENT-001", user.requesterId());
        assertEquals(Set.of("STUDENT"), user.roles());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_STUDENT".equals(authority.getAuthority())));
    }

    @Test
    void authenticate_rejectsTokenForDifferentAudience() {
        String reservationOnlyToken = tokenFor(
                "student01",
                "STUDENT-001",
                List.of("STUDENT"),
                List.of("reservation-service")
        );

        assertThrows(JwtException.class, () -> verifier.authenticate(reservationOnlyToken));
    }

    @Test
    void authenticate_rejectsTokenWithoutRoles() {
        String tokenWithoutRoles = Jwts.builder()
                .issuer(ISSUER)
                .subject("student01")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .claim("audiences", List.of("room-service"))
                .signWith(signingKey())
                .compact();

        assertThrows(JwtException.class, () -> verifier.authenticate(tokenWithoutRoles));
    }

    private String tokenFor(
            String username,
            String requesterId,
            List<String> roles,
            List<String> audiences
    ) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)))
                .claim("displayName", "Student One")
                .claim("requesterId", requesterId)
                .claim("roles", roles)
                .claim("audiences", audiences)
                .signWith(signingKey())
                .compact();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }
}
