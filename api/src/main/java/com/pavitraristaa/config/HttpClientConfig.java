package com.pavitraristaa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    // Spring Boot 4 no longer auto-configures RestClient.Builder without the restclient starter.
    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }
}
