package com.jaconis.bankflow.auth.client;

import com.jaconis.bankflow.auth.exception.AccountProvisioningException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AccountClientTest {

    private AccountClient accountClient;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8081");
        server = MockRestServiceServer.bindTo(builder).build();
        accountClient = new AccountClient(builder.build());
    }

    @Test
    void createDefaultAccount_ok_returnsAccountId() {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID accountId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        server.expect(requestTo("http://localhost:8081/accounts"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(AccountClient.USER_ID_HEADER, userId.toString()))
                .andRespond(withSuccess("""
                        {"id":"%s","userId":"%s","status":"ACTIVE","currency":"BRL","createdAt":"2026-08-20T12:00:00Z"}
                        """.formatted(accountId, userId), MediaType.APPLICATION_JSON));

        UUID result = accountClient.createDefaultAccount(userId);

        assertEquals(accountId, result);
        server.verify();
    }

    @Test
    void createDefaultAccount_whenAccountServiceFails_throwsProvisioningException() {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        server.expect(requestTo("http://localhost:8081/accounts"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThrows(AccountProvisioningException.class,
                () -> accountClient.createDefaultAccount(userId));
        server.verify();
    }
}
