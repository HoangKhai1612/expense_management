package com.finai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI financeAiOpenApi() {
        final String scheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Personal Finance AI System API")
                        .version("0.1.0")
                        .description("""
                                REST API for the Personal Finance AI System.

                                Conventions
                                - Authenticate with `Authorization: Bearer <token>` from POST /api/auth/login.
                                - Every error uses one envelope: { timestamp, status, code, message, path, violations }.
                                - Paginated collections use { content, page, size, totalElements, totalPages, first, last, empty }.
                                - Every user-scoped route returns only the authenticated caller's own data.
                                """)
                        .license(new License().name("Academic project")))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(scheme));
    }
}
