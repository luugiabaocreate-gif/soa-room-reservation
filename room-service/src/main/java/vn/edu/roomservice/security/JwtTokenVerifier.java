package vn.edu.roomservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtTokenVerifier {

    private final SecretKey signingKey;
    private final String issuer;
    private final String requiredAudience;

    public JwtTokenVerifier(
            @Value("${security.jwt.secret}") String base64Secret,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.audience}") String requiredAudience
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.issuer = issuer;
        this.requiredAudience = requiredAudience;
    }

    public UsernamePasswordAuthenticationToken authenticate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Set<String> audiences = claimValues(claims.get("audiences"));
        if (!audiences.contains(requiredAudience)) {
            throw new JwtException("JWT không dành cho service này");
        }

        Set<String> roles = claimValues(claims.get("roles"));
        if (roles.isEmpty()) {
            throw new JwtException("JWT không có role hợp lệ");
        }

        AuthenticatedUser user = new AuthenticatedUser(
                claims.getSubject(),
                claims.get("requesterId", String.class),
                claims.get("displayName", String.class),
                roles
        );

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        return new UsernamePasswordAuthenticationToken(user, token, authorities);
    }

    private Set<String> claimValues(Object claim) {
        if (!(claim instanceof Collection<?> values)) {
            return Set.of();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toUnmodifiableSet());
    }
}
