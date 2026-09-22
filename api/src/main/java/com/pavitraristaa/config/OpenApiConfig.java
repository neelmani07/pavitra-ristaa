package com.pavitraristaa.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pavitraOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pavitra Ristaa API")
                        .version("1.1.0")
                        .description("REST API for Dating/Love, Friendship and Marriage. Chat delivery uses WebSocket."))
                .servers(List.of(new Server().url("http://localhost:8080").description("Local development")))
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
