package com.jaconis.bankflow.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiConfigTest {

    @Test
    void openAPI_documentsGatewayRoutesAndBearerAuth() {
        OpenAPI openAPI = new OpenApiConfig().openAPI();

        assertEquals("BankFlow API Gateway", openAPI.getInfo().getTitle());
        assertNotNull(openAPI.getComponents().getSecuritySchemes().get("bearerAuth"));
        assertNotNull(openAPI.getComponents().getSchemas().get("ErrorResponse"));
        assertTrue(openAPI.getPaths().containsKey("/auth/login"));
        assertTrue(openAPI.getPaths().containsKey("/auth/me"));
        assertTrue(openAPI.getPaths().containsKey("/accounts"));
        assertTrue(openAPI.getPaths().containsKey("/transfers"));
        assertTrue(openAPI.getPaths().get("/auth/login").getPost().getSecurity().isEmpty());
        assertTrue(openAPI.getPaths().get("/auth/me").getGet().getSecurity().stream()
                .anyMatch(requirement -> requirement.containsKey("bearerAuth")));
    }
}
