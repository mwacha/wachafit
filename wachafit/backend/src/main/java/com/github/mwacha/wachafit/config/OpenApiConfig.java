package com.github.mwacha.wachafit.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String schemeName = "bearerAuth";
        return new OpenAPI()
            .info(new Info()
                .title("WachaFit API")
                .version("1.0.0")
                .description("""
                    API REST para gestão de academias e personal trainers.

                    **Autenticação:** Bearer JWT — faça login em `/api/auth/login` e passe o token
                    no header `Authorization: Bearer <token>`.

                    **Roles:** ADMIN · MANAGER · CASHIER · RECEPTIONIST · TRAINER · STUDENT
                    """)
                .contact(new Contact()
                    .name("WachaFit")
                    .email("mwacha@gmail.com")))
            .addSecurityItem(new SecurityRequirement().addList(schemeName))
            .components(new Components()
                .addSecuritySchemes(schemeName, new SecurityScheme()
                    .name(schemeName)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
