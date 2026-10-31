package com.lms.webportal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class PayosService {

    @Value("${payos.client-id:0cc855e6-ed8b-4eb5-a5a9-f5a4cee21208}")
    private String clientId;

    @Value("${payos.api-key:61bf9bfc-9aca-4729-ae1d-458a26e2f121}")
    private String apiKey;

    @Value("${payos.checksum-key:6083ce0b91cc434588afe5ce44becee7d71e1d4a5a426828cde6a77c43f214a9}")
    private String checksumKey;

    @Value("${payos.base-url:https://api-merchant.payos.vn/v2/payment-requests}")
    private String baseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    @Setter
    public static class PaymentLinkResult {
        private long orderCode;
        private String checkoutUrl;
        private String qrCode;
        private String status;
        private String paymentLinkId;
    }

    /**
     * Tạo chữ ký HMAC-SHA256 theo quy chuẩn PayOS
     * sorted: amount, cancelUrl, description, orderCode, returnUrl
     */
    public String createSignature(long amount, String cancelUrl, String description, long orderCode, String returnUrl) {
        try {
            String dataToSign = String.format("amount=%d&cancelUrl=%s&description=%s&orderCode=%d&returnUrl=%s",
                    amount, cancelUrl, description, orderCode, returnUrl);

            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(checksumKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Lỗi khi tạo chữ ký PayOS: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo chữ ký PayOS", e);
        }
    }

    /**
     * Tạo Payment Link qua PayOS API
     */
    public PaymentLinkResult createPaymentLink(long orderCode, long amount, String description, String cancelUrl, String returnUrl) {
        try {
            // PayOS description tối đa 25 ký tự, không dấu để hiển thị đẹp trên ngân hàng
            String sanitizedDesc = sanitizeDescription(description);

            String signature = createSignature(amount, cancelUrl, sanitizedDesc, orderCode, returnUrl);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("orderCode", orderCode);
            requestBody.put("amount", amount);
            requestBody.put("description", sanitizedDesc);
            requestBody.put("cancelUrl", cancelUrl);
            requestBody.put("returnUrl", returnUrl);
            requestBody.put("signature", signature);

            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Content-Type", "application/json")
                    .header("x-client-id", clientId)
                    .header("x-api-key", apiKey)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) EduLearnLMS/1.0")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("PayOS API lỗi HTTP {}: {}", response.statusCode(), response.body());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!"00".equals(root.path("code").asText())) {
                log.error("PayOS trả lời mã lỗi: {} - {}", root.path("code").asText(), root.path("desc").asText());
                return null;
            }

            JsonNode data = root.path("data");
            PaymentLinkResult result = new PaymentLinkResult();
            result.setOrderCode(data.path("orderCode").asLong());
            result.setCheckoutUrl(data.path("checkoutUrl").asText());
            result.setQrCode(data.path("qrCode").asText());
            result.setStatus(data.path("status").asText("PENDING"));
            result.setPaymentLinkId(data.path("paymentLinkId").asText());

            log.info("Tạo PayOS link thành công cho đơn hàng: orderCode={}, checkoutUrl={}", orderCode, result.getCheckoutUrl());
            return result;
        } catch (Exception e) {
            log.error("Ngoại lệ khi gọi PayOS createPaymentLink: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Tra cứu trạng thái đơn hàng PayOS
     * @return PAID, PENDING, CANCELLED, hoặc ERROR
     */
    public String getPaymentStatus(long orderCode) {
        try {
            String url = baseUrl + "/" + orderCode;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("x-client-id", clientId)
                    .header("x-api-key", apiKey)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) EduLearnLMS/1.0")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if ("00".equals(root.path("code").asText())) {
                    return root.path("data").path("status").asText("UNKNOWN");
                }
            }
            log.warn("Không lấy được trạng thái PayOS cho order {}: HTTP {}", orderCode, response.statusCode());
            return "ERROR";
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra trạng thái PayOS order {}: {}", orderCode, e.getMessage());
            return "ERROR";
        }
    }

    /**
     * Chuẩn hóa mô tả đơn hàng: PayOS yêu cầu tối đa 25 ký tự, không dấu
     */
    private String sanitizeDescription(String desc) {
        if (desc == null || desc.isBlank()) {
            return "Hoc phi EduLearn";
        }
        // Bỏ dấu tiếng Việt
        String nfd = java.text.Normalizer.normalize(desc, java.text.Normalizer.Form.NFD);
        String noAccents = nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        noAccents = noAccents.replaceAll("[^a-zA-Z0-9 ]", " ").trim().replaceAll("\\s+", " ");
        if (noAccents.length() > 25) {
            return noAccents.substring(0, 25).trim();
        }
        return noAccents.isEmpty() ? "Hoc phi EduLearn" : noAccents;
    }
}
