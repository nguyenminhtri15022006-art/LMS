package com.lms.webapp.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = GatewayExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GatewayExceptionHandlerTest.TestFailingController.class)
class GatewayExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Controller
    static class TestFailingController {

        @GetMapping("/test/service-unavailable")
        public String throw503() {
            throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "Gateway service unavailable");
        }

        @GetMapping("/test/bad-request")
        public String throw400() {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid input data");
        }

        @GetMapping("/test/not-found")
        public String throw404() {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Resource not found");
        }
        @GetMapping(value = "/test/json-service-unavailable", produces = MediaType.APPLICATION_JSON_VALUE)
        @ResponseBody
        public String throw503Json() {
            throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "Gateway service unavailable");
        }

        @GetMapping(value = "/test/json-4xx-sanitized", produces = MediaType.APPLICATION_JSON_VALUE)
        @ResponseBody
        public String throw400WithDebug() {
            String downstreamBody = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"Mã không hợp lệ\",\"debug\":\"http://private-service:8081; SQL_DIAGNOSTIC_MARKER\"}";
            throw HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY, downstreamBody.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.charset.StandardCharsets.UTF_8);
        }

        @GetMapping(value = "/test/json-4xx-non-json", produces = MediaType.APPLICATION_JSON_VALUE)
        @ResponseBody
        public String throw400WithNonJson() {
            String downstreamBody = "<html><body>502 Bad Gateway - upstream died</body></html>";
            throw HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY, downstreamBody.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("1. htmlRequestRenders503Page: HTML request gặp 503 từ Gateway -> render view error/503 với HTTP status 503 và nút thử lại")
    void htmlRequestRenders503Page() throws Exception {
        mockMvc.perform(get("/test/service-unavailable")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isServiceUnavailable())
                .andExpect(view().name("error/503"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("window.location.reload()")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thử lại")));
    }

    @Test
    @DisplayName("2. ajaxRequestReturns503Json: AJAX request gặp 503 từ Gateway -> trả JSON 503 với code 1405 không trả HTML")
    void ajaxRequestReturns503Json() throws Exception {
        mockMvc.perform(get("/test/service-unavailable")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value(1405))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Dịch vụ tạm thời không khả dụng")));
    }

    @Test
    @DisplayName("3. jsonEndpointReturns503JsonWithWildcardAccept: Endpoint produces JSON khi Accept: */* và không có header X-Requested-With vẫn trả JSON 503")
    void jsonEndpointReturns503JsonWithWildcardAccept() throws Exception {
        mockMvc.perform(get("/test/json-service-unavailable")
                        .accept(MediaType.ALL))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value(1405))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Dịch vụ tạm thời không khả dụng")));
    }

    @Test
    @DisplayName("4. business4xxNotConvertedTo503: Lỗi nghiệp vụ 400/404 không bị biến thành lỗi 503")
    void business4xxNotConvertedTo503() throws Exception {
        // 400 Bad Request
        mockMvc.perform(get("/test/bad-request")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // 404 Not Found
        mockMvc.perform(get("/test/not-found")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("5. client4xxErrorSanitizesDebugInfoViaMockMvc: MockMvc kiểm chứng lỗi 4xx lọc sạch thông tin nhạy cảm và URL nội bộ")
    void client4xxErrorSanitizesDebugInfoViaMockMvc() throws Exception {
        mockMvc.perform(get("/test/json-4xx-sanitized")
                        .accept(MediaType.ALL))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-service"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SQL_DIAGNOSTIC_MARKER"))))
                .andExpect(jsonPath("$.message").value("Mã không hợp lệ"));
    }

    @Test
    @DisplayName("6. client4xxErrorHandlesNonJsonBodyGracefully: Downstream trả 4xx với body HTML/text -> trả thông báo an toàn, không sinh lỗi mới")
    void client4xxErrorHandlesNonJsonBodyGracefully() throws Exception {
        mockMvc.perform(get("/test/json-4xx-non-json")
                        .accept(MediaType.ALL))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Dữ liệu yêu cầu không hợp lệ")));
    }
}
