package com.lms.webportal.config;

import com.lms.webportal.client.BearerTokenInterceptor;
import com.lms.webportal.client.TokenRefreshInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Arrays;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient gatewayRestClient(RestClient.Builder restClientBuilder,
                                       BearerTokenInterceptor bearerTokenInterceptor,
                                       TokenRefreshInterceptor tokenRefreshInterceptor,
                                       @Value("${lms.gateway.base-url:http://localhost:8080/api}") String baseUrl,
                                       Environment env) {
        if (!Arrays.asList(env.getActiveProfiles()).contains("test")) {
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
            requestFactory.setReadTimeout(Duration.ofSeconds(30));
            restClientBuilder.requestFactory(requestFactory);
        }

        return restClientBuilder
                .baseUrl(baseUrl)
                .requestInterceptor(tokenRefreshInterceptor)
                .requestInterceptor(bearerTokenInterceptor)
                .build();
    }
}
