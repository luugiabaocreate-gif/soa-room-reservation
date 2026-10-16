package vn.edu.auditservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class AuditServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}