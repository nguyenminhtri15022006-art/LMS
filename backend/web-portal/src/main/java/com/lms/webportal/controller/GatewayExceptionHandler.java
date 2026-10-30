package com.lms.webportal.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Xử lý tập trung các ngoại lệ kết nối giữa web-app (BFF) và API Gateway / Downstream.
 * Đảm bảo:
 * - 503 Service Unavailable:
 *   + Request HTML -> render view error/503 với HTTP status 503.
 *   + Request AJAX / JSON endpoint -> trả JSON 503 với code 1405, không trả HTML làm vỡ JavaScript.
 * - Lỗi nghiệp vụ 4xx (400, 401, 403, 404): giữ nguyên mã trạng thái 4xx, không biến thành 503.
 * - Khử trùng (sanitization) dữ liệu lỗi: chỉ trả về các trường nghiệp vụ được phép theo hợp đồng,
 *   tuyệt đối không để lộ stack trace, URL nội bộ (private-service), marker debug hay thông tin nhạy cảm.
 */
@ControllerAdvice
public class GatewayExceptionHandler {

    private static final int GATEWAY_SERVICE_UNAVAILABLE_CODE = 1405;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @ExceptionHandler({HttpServerErrorException.class, ResourceAccessException.class})
    public Object handleGatewayUnavailable(Exception ex, HttpServletRequest request) {
        if (isJsonResponseExpected(request)) {
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
        if (isJsonResponseExpected(request)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", ex.getStatusCode().value());
            body.put("error", ex.getStatusText());

            Integer businessCode = null;
            String businessMessage = null;

            String responseBody = ex.getResponseBodyAsString();
            if (responseBody != null && !responseBody.isBlank()) {
                try {
                    JsonNode node = objectMapper.readTree(responseBody);
                    if (node.isObject()) {
                        if (node.hasNonNull("code") && node.get("code").canConvertToInt()) {
                            businessCode = node.get("code").asInt();
                        }
                        if (node.hasNonNull("message") && node.get("message").isTextual()) {
                            String msg = node.get("message").asText().trim();
                            if (!msg.isBlank() && !containsSensitiveInfo(msg)) {
                                businessMessage = msg;
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // Body không phải JSON hoặc phân tích lỗi -> dùng fallback an toàn mặc định
                }
            }

            if (businessCode == null) {
                businessCode = ex.getStatusCode().value();
            }
            if (businessMessage == null) {
                businessMessage = getDefaultSafeMessage(ex.getStatusCode());
            }

            body.put("message", businessMessage);
            body.put("code", businessCode);

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

    /**
     * Xác định xem request có mong đợi phản hồi dạng JSON hay không.
     * Sử dụng các tiêu chí kết hợp:
     * 1. Metadata producible media types từ Spring MVC handler mapping.
     * 2. Metadata handler method (@ResponseBody, @RestController, ResponseEntity return type).
     * 3. Header AJAX (X-Requested-With: XMLHttpRequest).
     * 4. Header Accept chứa application/json.
     * 5. Đường dẫn endpoint API (/api/ hoặc .json).
     */
    private boolean isJsonResponseExpected(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        // 1. Kiểm tra request attribute PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE từ Spring MVC mapping
        Object producibleAttr = request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
        if (producibleAttr instanceof Set<?> set) {
            for (Object item : set) {
                if (item instanceof MediaType mt && mt.isCompatibleWith(MediaType.APPLICATION_JSON)) {
                    return true;
                }
            }
        }

        // 2. Kiểm tra HandlerMethod nếu có trong attribute của Spring MVC
        Object handlerAttr = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (handlerAttr instanceof HandlerMethod hm) {
            if (hm.hasMethodAnnotation(ResponseBody.class)
                    || hm.getBeanType().isAnnotationPresent(ResponseBody.class)
                    || hm.getBeanType().isAnnotationPresent(RestController.class)
                    || ResponseEntity.class.isAssignableFrom(hm.getMethod().getReturnType())) {
                return true;
            }
        }

        // 3. Kiểm tra Header X-Requested-With: XMLHttpRequest (AJAX)
        String xRequestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(xRequestedWith)) {
            return true;
        }

        // 4. Kiểm tra Header Accept
        String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);
        if (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE)) {
            return true;
        }

        // 5. Kiểm tra đường dẫn URI nếu là endpoint API
        String uri = request.getRequestURI();
        if (uri != null && (uri.startsWith("/api/") || uri.endsWith(".json"))) {
            return true;
        }

        return false;
    }

    private boolean containsSensitiveInfo(String text) {
        if (text == null) {
            return false;
        }
        String lower = text.toLowerCase();
        return lower.contains("http://")
                || lower.contains("https://")
                || lower.contains("select ")
                || lower.contains("insert ")
                || lower.contains("update ")
                || lower.contains("delete ")
                || lower.contains("from ")
                || lower.contains("stacktrace")
                || lower.contains("exception")
                || lower.contains("diagnostic")
                || lower.contains("private-service")
                || lower.contains("internal");
    }

    private String getDefaultSafeMessage(HttpStatusCode status) {
        if (status == HttpStatus.NOT_FOUND) {
            return "Không tìm thấy tài nguyên yêu cầu.";
        } else if (status == HttpStatus.FORBIDDEN) {
            return "Bạn không có quyền thực hiện thao tác này.";
        } else if (status == HttpStatus.UNAUTHORIZED) {
            return "Yêu cầu xác thực tài khoản.";
        } else if (status == HttpStatus.BAD_REQUEST) {
            return "Dữ liệu yêu cầu không hợp lệ.";
        }
        return "Yêu cầu không thể hoàn thành.";
    }
}
