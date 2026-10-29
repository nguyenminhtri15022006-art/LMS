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
        RestClient.RequestBodySpec spec = restClient.post().uri(uri);
        if (requestBody != null) {
            spec.body(requestBody);
        }
        return spec.retrieve().body(responseType);
    }

    public <T> T put(String uri, Object requestBody, Class<T> responseType) {
        RestClient.RequestBodySpec spec = restClient.put().uri(uri);
        if (requestBody != null) {
            spec.body(requestBody);
        }
        return spec.retrieve().body(responseType);
    }

    public <T> T patch(String uri, Object requestBody, Class<T> responseType) {
        RestClient.RequestBodySpec spec = restClient.patch().uri(uri);
        if (requestBody != null) {
            spec.body(requestBody);
        }
        return spec.retrieve().body(responseType);
    }

    public <T> T postMultipart(String uri, org.springframework.util.MultiValueMap<String, Object> body, Class<T> responseType) {
        return restClient.post()
                .uri(uri)
                .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(responseType);
    }

    public <T> T putMultipart(String uri, org.springframework.util.MultiValueMap<String, Object> body, Class<T> responseType) {
        return restClient.put()
                .uri(uri)
                .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(responseType);
    }

    public <T> T delete(String uri, Class<T> responseType) {
        return restClient.delete()
                .uri(uri)
                .retrieve()
                .body(responseType);
    }
}
