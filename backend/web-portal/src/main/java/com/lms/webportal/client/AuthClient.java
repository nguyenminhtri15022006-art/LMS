package com.lms.webportal.client;

import com.lms.webportal.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "auth-client", url = "${app.gateway-url:http://localhost:8080}")
public interface AuthClient {

    @PostMapping("/api/identity/auth/login")
    ApiResponse<Map<String, Object>> login(@RequestBody Map<String, Object> request);

    @PostMapping("/api/identity/auth/register")
    ApiResponse<Map<String, Object>> register(@RequestBody Map<String, Object> request);
}
