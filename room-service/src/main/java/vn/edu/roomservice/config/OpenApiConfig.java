package vn.edu.roomservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI roomServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Room Service API")
                        .version("v1 + v2, secured in TH5")
                        .description("API quản lý phòng học. TH5 yêu cầu Bearer JWT cho Room APIs; "
                                + "chỉ ROOM_MANAGER hoặc ADMIN được thay đổi Room."))
                .components(new Components().addSecuritySchemes(
                        BEARER_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
