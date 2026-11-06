package vn.edu.authservice.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthDataInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthDataInitializer.class);

    @Bean
    ApplicationRunner seedDemoUsers(
            AuthUserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            createIfAbsent(userRepository, passwordEncoder,
                    "student01", "Student One", AppRole.STUDENT, "STUDENT-001");
            createIfAbsent(userRepository, passwordEncoder,
                    "student02", "Student Two", AppRole.STUDENT, "STUDENT-002");
            createIfAbsent(userRepository, passwordEncoder,
                    "roommanager01", "Room Manager", AppRole.ROOM_MANAGER, null);
            createIfAbsent(userRepository, passwordEncoder,
                    "admin01", "System Administrator", AppRole.ADMIN, null);
        };
    }

    private void createIfAbsent(
            AuthUserRepository repository,
            PasswordEncoder passwordEncoder,
            String username,
            String displayName,
            AppRole role,
            String requesterId
    ) {
        if (repository.existsByUsername(username)) {
            return;
        }

        repository.save(new AuthUserEntity(
                username,
                passwordEncoder.encode("DemoPassword123!"),
                displayName,
                role,
                requesterId,
                true
        ));
        LOGGER.info("Seeded local demo account username={}, role={}", username, role);
    }
}
