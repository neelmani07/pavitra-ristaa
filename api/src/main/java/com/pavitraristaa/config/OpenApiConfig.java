package com.pavitraristaa.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pavitraOpenApi() {
        // Deliberately no .servers(...) here. A hardcoded server URL (this used to say
        // http://localhost:8080) becomes wrong the moment the spec is served from anywhere else -
        // which broke Swagger UI's "Try it out" in staging: it kept building requests against
        // localhost:8080 on whoever's own machine had the tab open, over plain HTTP from an HTTPS
        // page, which fails in a browser in a way that surfaces as a generic CORS/fetch error with
        // no obvious connection to the real cause. Springdoc auto-derives the server URL from the
        // incoming request when none is declared, so this now works correctly in local dev, staging,
        // and production without any per-environment configuration at all.
        return new OpenAPI()
                .info(new Info()
                        .title("Pavitra Ristaa API")
                        .version("1.1.0")
                        .description("REST API for Dating/Love, Friendship and Marriage. Chat delivery uses WebSocket."))
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
