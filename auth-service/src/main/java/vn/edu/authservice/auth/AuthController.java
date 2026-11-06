package vn.edu.authservice.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication API", description = "JWT login và service credential cho TH5")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Xác thực username/password và cấp access token JWT 15 phút")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/service-token")
    @Operation(summary = "Cấp machine token cho Reservation Service gọi Room Service")
    public ResponseEntity<TokenResponse> serviceToken(
            @Valid @RequestBody ServiceTokenRequest request
    ) {
        return ResponseEntity.ok(authService.issueReservationServiceToken(request));
    }
}
