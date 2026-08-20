package com.jaconis.bankflow.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AccountClientConfig {

    @Bean
    RestClient accountRestClient(
            RestClient.Builder builder,
            @Value("${app.account-service.base-url}") String baseUrl
    ) {
        return builder.baseUrl(baseUrl).build();
    }
}
