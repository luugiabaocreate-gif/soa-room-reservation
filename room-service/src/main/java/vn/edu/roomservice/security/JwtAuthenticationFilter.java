package vn.edu.roomservice.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenVerifier tokenVerifier;
    private final SecurityErrorResponseWriter errorWriter;

    public JwtAuthenticationFilter(
            JwtTokenVerifier tokenVerifier,
            SecurityErrorResponseWriter errorWriter
    ) {
        this.tokenVerifier = tokenVerifier;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith("Bearer ")) {
            errorWriter.writeUnauthorized(request, response,
                    "Authorization phải có dạng Bearer <JWT>");
            return;
        }

        try {
            var authentication = tokenVerifier.authenticate(authorization.substring(7).trim());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            errorWriter.writeUnauthorized(request, response,
                    "JWT không hợp lệ, hết hạn hoặc không dành cho service này");
        }
    }
}
