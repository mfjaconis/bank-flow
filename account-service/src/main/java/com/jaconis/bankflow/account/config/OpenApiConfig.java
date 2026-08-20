package com.jaconis.bankflow.account.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Account Service")
                        .version("1.0")
                        .description("""
                                Contas do BankFlow: abertura, consulta e saldo.
                                Via API Gateway, o JWT é validado e o header X-User-Id é propagado.
                                Em acesso direto ao serviço, envie X-User-Id manualmente.
                                """))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components()
                        .addSecuritySchemes(BEARER,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT validado no API Gateway"))
                        .addParameters("X-User-Id",
                                new Parameter()
                                        .in("header")
                                        .name("X-User-Id")
                                        .required(true)
                                        .description("ID do usuário autenticado (UUID)")));
    }
}
