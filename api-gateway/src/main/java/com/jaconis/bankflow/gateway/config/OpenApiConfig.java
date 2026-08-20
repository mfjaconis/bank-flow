package com.jaconis.bankflow.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";
    private static final String JSON = "application/json";

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BankFlow API Gateway")
                        .version("1.0")
                        .description("""
                                Porta única do BankFlow (:8080).
                                As rotas abaixo são proxied para auth-service, account-service e transfer-service.
                                JWT é validado na borda (exceto login/register). Header X-Request-Id é gerado ou propagado.
                                """))
                .addTagsItem(new Tag().name("Auth").description("Proxy → auth-service (:8084)"))
                .addTagsItem(new Tag().name("Accounts").description("Proxy → account-service (:8081)"))
                .addTagsItem(new Tag().name("Transfers").description("Proxy → transfer-service (:8082)"))
                .addTagsItem(new Tag().name("Gateway").description("Endpoints locais do gateway"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT emitido por POST /auth/login"))
                        .addSchemas("ErrorResponse", errorSchema()))
                .path("/auth/register", new PathItem().post(publicOp("Auth", "Registrar usuário")
                        .responses(responses(Map.of(
                                "201", "Criado",
                                "400", "Validação",
                                "409", "E-mail já cadastrado",
                                "429", "Rate limit"
                        )))))
                .path("/auth/login", new PathItem().post(publicOp("Auth", "Login → JWT")
                        .responses(responses(Map.of(
                                "200", "OK",
                                "401", "Credenciais inválidas",
                                "429", "Rate limit"
                        )))))
                .path("/auth/me", new PathItem().get(securedOp("Auth", "Usuário autenticado")
                        .responses(responses(Map.of(
                                "200", "OK",
                                "401", "Token ausente ou inválido"
                        )))))
                .path("/accounts", new PathItem().post(securedOp("Accounts", "Abrir conta")
                        .responses(responses(Map.of(
                                "201", "Criada",
                                "401", "Não autenticado"
                        )))))
                .path("/accounts/{id}", new PathItem().get(securedOp("Accounts", "Consultar conta")
                        .addParametersItem(idParam())
                        .responses(responses(Map.of(
                                "200", "OK",
                                "401", "Não autenticado",
                                "404", "Não encontrada"
                        )))))
                .path("/accounts/{id}/balance", new PathItem().get(securedOp("Accounts", "Saldo da conta")
                        .addParametersItem(idParam())
                        .responses(responses(Map.of(
                                "200", "OK",
                                "401", "Não autenticado"
                        )))))
                .path("/accounts/{id}/statement", new PathItem().get(securedOp("Accounts", "Extrato (ledger)")
                        .addParametersItem(idParam())
                        .responses(responses(Map.of(
                                "200", "OK",
                                "401", "Não autenticado"
                        )))))
                .path("/transfers", new PathItem().post(securedOp("Transfers", "Criar transferência")
                        .addParametersItem(new HeaderParameter()
                                .name("Idempotency-Key")
                                .required(true)
                                .schema(new Schema<>().type("string"))
                                .description("Chave de idempotência (obrigatória)"))
                        .responses(responses(Map.of(
                                "201", "Aceita / criada",
                                "202", "Pendente (assíncrono)",
                                "401", "Não autenticado",
                                "409", "Idempotency-Key com body diferente"
                        )))))
                .path("/transfers/{id}", new PathItem().get(securedOp("Transfers", "Consultar transferência")
                        .addParametersItem(idParam())
                        .responses(responses(Map.of(
                                "200", "OK",
                                "401", "Não autenticado",
                                "404", "Não encontrada"
                        )))))
                .path("/actuator/health", new PathItem().get(publicOp("Gateway", "Health check")
                        .responses(responses(Map.of("200", "UP")))));
    }

    private static Operation publicOp(String tag, String summary) {
        return new Operation()
                .addTagsItem(tag)
                .summary(summary)
                .addParametersItem(requestIdParam())
                .security(List.of());
    }

    private static Operation securedOp(String tag, String summary) {
        return new Operation()
                .addTagsItem(tag)
                .summary(summary)
                .addParametersItem(requestIdParam())
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    private static Parameter requestIdParam() {
        return new HeaderParameter()
                .name("X-Request-Id")
                .required(false)
                .schema(new Schema<>().type("string").format("uuid"))
                .description("Correlation id; gerado pelo gateway se ausente");
    }

    private static Parameter idParam() {
        return new PathParameter()
                .name("id")
                .required(true)
                .schema(new Schema<>().type("string").format("uuid"));
    }

    private static ApiResponses responses(Map<String, String> codeToDescription) {
        ApiResponses apiResponses = new ApiResponses();
        codeToDescription.forEach((code, description) ->
                apiResponses.addApiResponse(code, new ApiResponse()
                        .description(description)
                        .content(new Content().addMediaType(JSON, new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))))));
        return apiResponses;
    }

    @SuppressWarnings("rawtypes")
    private static Schema errorSchema() {
        return new Schema<>()
                .type("object")
                .addProperty("timestamp", new Schema<>().type("string").format("date-time"))
                .addProperty("status", new Schema<>().type("integer"))
                .addProperty("message", new Schema<>().type("string"))
                .addProperty("path", new Schema<>().type("string"));
    }
}
