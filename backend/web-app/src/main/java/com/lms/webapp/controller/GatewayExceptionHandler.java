package com.lms.webapp.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.ModelAndView;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Xử lý tập trung các ngoại lệ kết nối giữa web-app (BFF) và API Gateway / Downstream.
 * Đảm bảo:
 * - 503 Service Unavailable:
 *   + Request HTML -> render view error/503 với HTTP status 503.
 *   + Request AJAX -> trả JSON 503 với code 1405, không trả HTML làm vỡ JavaScript.
 * - Lỗi nghiệp vụ 4xx (400, 401, 403, 404): giữ nguyên mã trạng thái 4xx, không biến thành 503.
 * - Không làm lộ stack trace hoặc thông tin nội bộ hệ thống.
 */
@ControllerAdvice
public class GatewayExceptionHandler {

    private static final int GATEWAY_SERVICE_UNAVAILABLE_CODE = 1405;

    @ExceptionHandler({HttpServerErrorException.class, ResourceAccessException.class})
    public Object handleGatewayUnavailable(Exception ex, HttpServletRequest request) {
        if (isAjaxRequest(request)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
            body.put("error", "Service Unavailable");
            body.put("message", "Dịch vụ tạm thời không khả dụng. Vui lòng thử lại sau.");
            body.put("code", GATEWAY_SERVICE_UNAVAILABLE_CODE);

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
        }

        ModelAndView mav = new ModelAndView("error/503");
        mav.setStatus(HttpStatus.SERVICE_UNAVAILABLE);
        return mav;
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public Object handleHttpClientError(HttpClientErrorException ex, HttpServletRequest request) {
        // Giữ nguyên HTTP status code 4xx từ downstream/Gateway, tuyệt đối không chuyển thành 503
        if (isAjaxRequest(request)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", ex.getStatusCode().value());
            body.put("error", ex.getStatusText());
            body.put("message", ex.getMessage());

            return ResponseEntity.status(ex.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
        }

        // Với HTML request, nếu có trang lỗi cụ thể (403, 404) thì điều hướng, hoặc trả status tương ứng
        int status = ex.getStatusCode().value();
        if (status == 404) {
            ModelAndView mav = new ModelAndView("error/404");
            mav.setStatus(HttpStatus.NOT_FOUND);
            return mav;
        } else if (status == 403) {
            ModelAndView mav = new ModelAndView("error/403");
            mav.setStatus(HttpStatus.FORBIDDEN);
            return mav;
        }

        return ResponseEntity.status(ex.getStatusCode()).build();
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String xRequestedWith = request.getHeader("X-Requested-With");
        String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);

        return "XMLHttpRequest".equalsIgnoreCase(xRequestedWith)
                || (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE));
    }
}
