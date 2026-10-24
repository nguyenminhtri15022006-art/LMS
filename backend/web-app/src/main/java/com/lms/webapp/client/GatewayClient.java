package com.lms.webapp.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GatewayClient {

    private final RestClient restClient;

    public GatewayClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public <T> T get(String uri, Class<T> responseType) {
        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(responseType);
    }

    public <T> T post(String uri, Object requestBody, Class<T> responseType) {
        return restClient.post()
                .uri(uri)
                .body(requestBody)
                .retrieve()
                .body(responseType);
    }
}
