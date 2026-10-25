package vn.edu.roomservice.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            SecurityErrorResponseWriter errorWriter
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                errorWriter.writeUnauthorized(request, response,
                                        "Cần đăng nhập bằng Bearer JWT để gọi API này"))
                        .accessDeniedHandler((request, response, exception) ->
                                errorWriter.writeForbidden(request, response,
                                        "Tài khoản đã đăng nhập nhưng không có quyền thực hiện thao tác này")))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api/v1/rooms/hello",
                                "/api/v1/rooms/info"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/rooms")
                        .hasAnyRole("ROOM_MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/rooms/**")
                        .hasAnyRole("ROOM_MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/rooms/**")
                        .hasAnyRole("ROOM_MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/rooms/**", "/api/v2/rooms/**")
                        .authenticated()
                        .anyRequest().denyAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
