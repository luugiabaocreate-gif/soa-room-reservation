package vn.edu.authservice.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.authservice.security.JwtTokenService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class AuthService {

    private final AuthUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final String reservationClientId;
    private final String reservationClientSecret;

    public AuthService(
            AuthUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            @Value("${security.internal.reservation-client-id}") String reservationClientId,
            @Value("${security.internal.reservation-client-secret}") String reservationClientSecret
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.reservationClientId = reservationClientId;
        this.reservationClientSecret = reservationClientSecret;
    }

    public TokenResponse login(LoginRequest request) {
        AuthUserEntity user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return jwtTokenService.issueUserToken(user);
    }

    public TokenResponse issueReservationServiceToken(ServiceTokenRequest request) {
        boolean validClientId = MessageDigest.isEqual(
                reservationClientId.getBytes(StandardCharsets.UTF_8),
                request.clientId().getBytes(StandardCharsets.UTF_8)
        );
        boolean validSecret = MessageDigest.isEqual(
                reservationClientSecret.getBytes(StandardCharsets.UTF_8),
                request.clientSecret().getBytes(StandardCharsets.UTF_8)
        );

        if (!validClientId || !validSecret) {
            throw new InvalidCredentialsException();
        }

        return jwtTokenService.issueReservationServiceToken();
    }
}
