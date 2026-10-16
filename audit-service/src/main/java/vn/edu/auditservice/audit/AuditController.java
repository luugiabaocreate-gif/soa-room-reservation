package vn.edu.auditservice.audit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/audits")
public class AuditController {

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of(
                "service", "Audit Service",
                "status", "UP",
                "message", "Audit consumer is ready"
        );
    }
}