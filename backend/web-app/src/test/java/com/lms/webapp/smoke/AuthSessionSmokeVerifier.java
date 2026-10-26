package com.lms.webapp.smoke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class AuthSessionSmokeVerifier {

    private static final Logger log = LoggerFactory.getLogger(AuthSessionSmokeVerifier.class);

    public static class LoginProfileResult {
        private final String sessionCookie;
        private final int httpStatus;

        public LoginProfileResult(String sessionCookie, int httpStatus) {
            this.sessionCookie = sessionCookie;
            this.httpStatus = httpStatus;
        }

        public String getSessionCookie() {
            return sessionCookie;
        }

        public int getHttpStatus() {
            return httpStatus;
        }
    }

    public LoginProfileResult executeLoginAndFetchProfile(String baseUrl, String username, String password) {
        String cleanBaseUrl = baseUrl.replaceAll("/+$", "");
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient httpClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        try {
            // 1. Lấy trang login để trích xuất CSRF token nếu có
            HttpRequest getLoginReq = HttpRequest.newBuilder()
                    .uri(URI.create(cleanBaseUrl + "/login"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> loginPageRes = httpClient.send(getLoginReq, HttpResponse.BodyHandlers.ofString());
            String html = loginPageRes.body();
            String csrfToken = extractCsrfToken(html);

            // 2. Gửi request POST /login
            StringBuilder formBody = new StringBuilder();
            formBody.append("username=").append(URLEncoder.encode(username, StandardCharsets.UTF_8));
            formBody.append("&password=").append(URLEncoder.encode(password, StandardCharsets.UTF_8));
            if (csrfToken != null) {
                formBody.append("&_csrf=").append(URLEncoder.encode(csrfToken, StandardCharsets.UTF_8));
            }

            HttpRequest loginPostReq = HttpRequest.newBuilder()
                    .uri(URI.create(cleanBaseUrl + "/login"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody.toString()))
                    .build();

            HttpResponse<String> loginRes = httpClient.send(loginPostReq, HttpResponse.BodyHandlers.ofString());

            // 3. Trích xuất JSESSIONID
            List<String> setCookies = loginRes.headers().allValues("Set-Cookie");
            String jsessionId = null;
            for (String cookie : setCookies) {
                if (cookie.contains("JSESSIONID=")) {
                    jsessionId = cookie;
                    break;
                }
            }

            // 4. Gửi GET /profile
            HttpRequest.Builder profileReqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(cleanBaseUrl + "/profile"))
                    .timeout(Duration.ofSeconds(10))
                    .GET();

            if (jsessionId != null) {
                profileReqBuilder.header("Cookie", jsessionId);
            }

            HttpResponse<String> profileRes = httpClient.send(profileReqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            return new LoginProfileResult(jsessionId, profileRes.statusCode());

        } catch (Exception e) {
            log.error("Smoke test login flow failed at {}: {}", cleanBaseUrl, e.getMessage());
            return new LoginProfileResult(null, -1);
        }
    }

    private String extractCsrfToken(String html) {
        if (html == null) return null;
        int idx = html.indexOf("name=\"_csrf\"");
        if (idx == -1) idx = html.indexOf("name='_csrf'");
        if (idx != -1) {
            int valIdx = html.indexOf("value=\"", idx);
            if (valIdx != -1) {
                int endIdx = html.indexOf("\"", valIdx + 7);
                if (endIdx != -1) {
                    return html.substring(valIdx + 7, endIdx);
                }
            }
        }
        return null;
    }
}
