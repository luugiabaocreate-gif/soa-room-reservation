package vn.edu.authservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.edu.authservice.auth.AuthUserEntity;
import vn.edu.authservice.auth.TokenResponse;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class JwtTokenService {

    private final SecretKey signingKey;
    private final String issuer;
    private final long expirationSeconds;

    public JwtTokenService(
            @Value("${security.jwt.secret}") String base64Secret,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.expiration-seconds}") long expirationSeconds
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
    }

    public TokenResponse issueUserToken(AuthUserEntity user) {
        return issue(
                user.getUsername(),
                user.getRequesterId(),
                user.getDisplayName(),
                List.of(user.getRole().name()),
                List.of("room-service", "reservation-service")
        );
    }

    public TokenResponse issueReservationServiceToken() {
        return issue(
                "reservation-service",
                null,
                "Reservation Service",
                List.of("SERVICE_RESERVATION"),
                List.of("room-service")
        );
    }

    private TokenResponse issue(
            String subject,
            String requesterId,
            String displayName,
            List<String> roles,
            List<String> audiences
    ) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);

        var builder = Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("displayName", displayName)
                .claim("roles", roles)
                .claim("audiences", audiences);

        if (requesterId != null) {
            builder.claim("requesterId", requesterId);
        }

        return new TokenResponse(
                builder.signWith(signingKey).compact(),
                "Bearer",
                expirationSeconds
        );
    }
}
