package com.jaconis.bankflow.auth.client;

import com.jaconis.bankflow.auth.exception.AccountProvisioningException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class AccountClient {

    public static final String USER_ID_HEADER = "X-User-Id";

    private final RestClient accountRestClient;

    public AccountClient(RestClient accountRestClient) {
        this.accountRestClient = accountRestClient;
    }

    public UUID createDefaultAccount(UUID userId) {
        try {
            AccountCreatedResponse response = accountRestClient.post()
                    .uri("/accounts")
                    .header(USER_ID_HEADER, userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{}")
                    .retrieve()
                    .body(AccountCreatedResponse.class);

            if (response == null || response.id() == null) {
                throw new AccountProvisioningException();
            }
            return response.id();
        } catch (RestClientException ex) {
            throw new AccountProvisioningException(ex);
        }
    }
}
