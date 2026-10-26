package com.lms.webapp.config;

import com.lms.webapp.client.BearerTokenInterceptor;
import com.lms.webapp.client.TokenRefreshInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient gatewayRestClient(RestClient.Builder restClientBuilder,
                                       BearerTokenInterceptor bearerTokenInterceptor,
                                       TokenRefreshInterceptor tokenRefreshInterceptor,
                                       @Value("${lms.gateway.base-url:http://localhost:8080/api}") String baseUrl) {
        return restClientBuilder
                .baseUrl(baseUrl)
                .requestInterceptor(tokenRefreshInterceptor)
                .requestInterceptor(bearerTokenInterceptor)
                .build();
    }
}
