package com.kodilla.portfolio.ui.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Builds the HTTP client used to reach the backend. */
@Configuration
public class BackendClientConfig {

    @Bean
    public RestClient backendRestClient(
            @Value("${app.backend.base-url}") String baseUrl,
            @Value("${app.backend.timeout-seconds}") int timeoutSeconds) {

        Duration timeout = Duration.ofSeconds(timeoutSeconds);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect()
                        .build(ClientHttpRequestFactorySettings.defaults()
                                .withConnectTimeout(timeout)
                                .withReadTimeout(timeout)))
                .build();
    }
}
