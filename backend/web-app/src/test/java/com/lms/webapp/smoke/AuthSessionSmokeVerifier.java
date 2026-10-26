package com.lms.webapp.smoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthSessionSmokeVerifier {

    private static final Logger log = LoggerFactory.getLogger(AuthSessionSmokeVerifier.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class LoginProfileResult {
        private final boolean success;
        private final String cleanSessionCookie;
        private final int httpStatus;
        private final String username;
        private final boolean profileVerified;
        private final String errorMessage;

        public LoginProfileResult(boolean success, String cleanSessionCookie, int httpStatus,
                                  String username, boolean profileVerified, String errorMessage) {
            this.success = success;
            this.cleanSessionCookie = cleanSessionCookie;
            this.httpStatus = httpStatus;
            this.username = username;
            this.profileVerified = profileVerified;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getCleanSessionCookie() {
            return cleanSessionCookie;
        }

        public int getHttpStatus() {
            return httpStatus;
        }

        public String getUsername() {
            return username;
        }

        public boolean isProfileVerified() {
            return profileVerified;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Kiểm tra truy cập ẩn danh (anonymous) bị từ chối chuyển hướng tới /login khi vào /profile
     */
    public boolean verifyAnonymousAccessDenied(String webAppBaseUrl) {
        String cleanBaseUrl = webAppBaseUrl.replaceAll("/+$", "");
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(cleanBaseUrl + "/profile"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String location = response.headers().firstValue("Location").orElse("");
            return status == 302 && location.contains("/login");
        } catch (Exception e) {
            log.error("Failed to verify anonymous access denial at {}: {}", cleanBaseUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Kiểm tra đăng nhập với thông tin sai bị từ chối
     */
    public boolean verifyInvalidLoginRejected(String webAppBaseUrl, String username, String badPassword) {
        String cleanBaseUrl = webAppBaseUrl.replaceAll("/+$", "");
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        try {
            // Lấy CSRF token từ trang login
            HttpRequest getLoginReq = HttpRequest.newBuilder()
                    .uri(URI.create(cleanBaseUrl + "/login"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> loginPageRes = client.send(getLoginReq, HttpResponse.BodyHandlers.ofString());
            String csrfToken = extractCsrfToken(loginPageRes.body());

            String formBody = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                    + "&password=" + URLEncoder.encode(badPassword, StandardCharsets.UTF_8)
                    + (csrfToken != null ? "&_csrf=" + URLEncoder.encode(csrfToken, StandardCharsets.UTF_8) : "");

            HttpRequest postLoginReq = HttpRequest.newBuilder()
                    .uri(URI.create(cleanBaseUrl + "/login"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> loginRes = client.send(postLoginReq, HttpResponse.BodyHandlers.ofString());
            int status = loginRes.statusCode();
            String location = loginRes.headers().firstValue("Location").orElse("");

            // Đăng nhập thất bại: trả về 200 (render form có lỗi) hoặc 302 redirect về /login?error
            if (status == 200 && (loginRes.body().contains("không chính xác") || loginRes.body().contains("loginError"))) {
                return true;
            }
            return status == 302 && location.contains("/login");
        } catch (Exception e) {
            log.error("Failed to verify invalid login rejection at {}: {}", cleanBaseUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Thực hiện đăng nhập chuẩn bằng CookieManager, kiểm tra chuyển hướng thành công
     * và truy cập profile của đúng người dùng (assert nội dung trả về có chứa username).
     */
    public LoginProfileResult executeLoginAndFetchProfile(String webAppBaseUrl, String username, String password) {
        String cleanBaseUrl = webAppBaseUrl.replaceAll("/+$", "");
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        try {
            // 1. GET /login để lấy CSRF token
            HttpRequest getLoginReq = HttpRequest.newBuilder()
                    .uri(URI.create(cleanBaseUrl + "/login"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> loginPageRes = client.send(getLoginReq, HttpResponse.BodyHandlers.ofString());
            String csrfToken = extractCsrfToken(loginPageRes.body());

            // 2. POST /login với credentials
            String formBody = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                    + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8)
                    + (csrfToken != null ? "&_csrf=" + URLEncoder.encode(csrfToken, StandardCharsets.UTF_8) : "");

            HttpRequest postLoginReq = HttpRequest.newBuilder()
                    .uri(URI.create(cleanBaseUrl + "/login"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> loginRes = client.send(postLoginReq, HttpResponse.BodyHandlers.ofString());
            int loginStatus = loginRes.statusCode();
            String location = loginRes.headers().firstValue("Location").orElse("");

            // Đăng nhập thành công trả về redirect 302 tới /
            if (loginStatus != 302 || location.contains("error") || !location.equals("/") && !location.isEmpty()) {
                return new LoginProfileResult(false, null, loginStatus, username, false,
                        "Login failed: expected redirect to /, got status " + loginStatus + " location: " + location);
            }

            // 3. Trích xuất cookie JSESSIONID dạng name=value sạch từ CookieStore
            String cleanJsessionId = null;
            List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
            for (HttpCookie c : cookies) {
                if ("JSESSIONID".equalsIgnoreCase(c.getName())) {
                    cleanJsessionId = c.getName() + "=" + c.getValue();
                    break;
                }
            }

            // Fallback nếu CookieStore chưa parse
            if (cleanJsessionId == null) {
                List<String> setCookies = loginRes.headers().allValues("Set-Cookie");
                for (String sc : setCookies) {
                    if (sc.contains("JSESSIONID=")) {
                        int start = sc.indexOf("JSESSIONID=");
                        int end = sc.indexOf(";", start);
                        cleanJsessionId = (end != -1) ? sc.substring(start, end).trim() : sc.substring(start).trim();
                        break;
                    }
                }
            }

            // 4. GET /profile sử dụng session đã đăng nhập
            HttpRequest getProfileReq = HttpRequest.newBuilder()
                    .uri(URI.create(cleanBaseUrl + "/profile"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> profileRes = client.send(getProfileReq, HttpResponse.BodyHandlers.ofString());
            int profileStatus = profileRes.statusCode();
            if (profileStatus != 200) {
                return new LoginProfileResult(false, cleanJsessionId, profileStatus, username, false,
                        "Profile fetch failed with status " + profileStatus);
            }

            String profileHtml = profileRes.body();
            // Xác minh profile chứa đúng thông tin người dùng đăng nhập
            boolean usernameMatched = profileHtml.contains(username);
            if (!usernameMatched) {
                return new LoginProfileResult(false, cleanJsessionId, profileStatus, username, false,
                        "Profile HTML does not contain expected username: " + username);
            }

            return new LoginProfileResult(true, cleanJsessionId, profileStatus, username, true, null);

        } catch (Exception e) {
            log.error("Login and profile smoke test exception at {}: {}", cleanBaseUrl, e.getMessage(), e);
            return new LoginProfileResult(false, null, -1, username, false, e.getMessage());
        }
    }

    /**
     * Chuẩn bị tài khoản kiểm thử thông qua API đăng ký và xác thực OTP từ Mailpit
     */
    public boolean registerAndVerifyAccountViaMailpit(String webAppBaseUrl, String mailpitBaseUrl,
                                                      String username, String email, String password) {
        String cleanBaseUrl = webAppBaseUrl.replaceAll("/+$", "");
        String cleanMailpitUrl = mailpitBaseUrl.replaceAll("/+$", "");

        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        try {
            // 1. GET /register lấy CSRF
            HttpRequest getRegReq = HttpRequest.newBuilder()
                    .uri(URI.create(cleanBaseUrl + "/register"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> regPageRes = client.send(getRegReq, HttpResponse.BodyHandlers.ofString());
            String csrfToken = extractCsrfToken(regPageRes.body());

            // 2. POST /register
            String formBody = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                    + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                    + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8)
                    + "&confirmPassword=" + URLEncoder.encode(password, StandardCharsets.UTF_8)
                    + (csrfToken != null ? "&_csrf=" + URLEncoder.encode(csrfToken, StandardCharsets.UTF_8) : "");

            HttpRequest postRegReq = HttpRequest.newBuilder()
                    .uri(URI.create(cleanBaseUrl + "/register"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> postRegRes = client.send(postRegReq, HttpResponse.BodyHandlers.ofString());
            if (postRegRes.statusCode() != 302) {
                log.warn("Registration POST failed with status {}: {}", postRegRes.statusCode(), postRegRes.body());
                return false;
            }

            // 3. Đọc mã OTP từ Mailpit API (/api/v1/messages)
            String otp = extractOtpFromMailpit(cleanMailpitUrl, email, client);
            if (otp == null) {
                log.warn("Could not find OTP email in Mailpit for {}", email);
                return false;
            }

            // 4. POST /verify-email với mã OTP
            String verifyBody = "email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                    + "&otp=" + URLEncoder.encode(otp, StandardCharsets.UTF_8)
                    + (csrfToken != null ? "&_csrf=" + URLEncoder.encode(csrfToken, StandardCharsets.UTF_8) : "");

            HttpRequest postVerifyReq = HttpRequest.newBuilder()
                    .uri(URI.create(cleanBaseUrl + "/verify-email"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(verifyBody))
                    .build();

            HttpResponse<String> verifyRes = client.send(postVerifyReq, HttpResponse.BodyHandlers.ofString());
            return verifyRes.statusCode() == 302;

        } catch (Exception e) {
            log.error("Register and verify account flow failed: {}", e.getMessage());
            return false;
        }
    }

    private String extractOtpFromMailpit(String mailpitBaseUrl, String email, HttpClient client) {
        try {
            HttpRequest getMessagesReq = HttpRequest.newBuilder()
                    .uri(URI.create(mailpitBaseUrl + "/api/v1/messages"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(getMessagesReq, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode messages = root.has("messages") ? root.get("messages") : root;
            if (messages.isArray()) {
                for (JsonNode msg : messages) {
                    String to = msg.has("To") ? msg.get("To").toString() : "";
                    if (to.contains(email)) {
                        String id = msg.get("ID").asText();
                        // Lấy nội dung chi tiết
                        HttpRequest getDetailReq = HttpRequest.newBuilder()
                                .uri(URI.create(mailpitBaseUrl + "/api/v1/message/" + id))
                                .timeout(Duration.ofSeconds(10))
                                .GET()
                                .build();
                        HttpResponse<String> detailRes = client.send(getDetailReq, HttpResponse.BodyHandlers.ofString());
                        String body = detailRes.body();
                        // Trích xuất 6 chữ số OTP
                        Pattern pattern = Pattern.compile("\\b(\\d{6})\\b");
                        Matcher matcher = pattern.matcher(body);
                        if (matcher.find()) {
                            return matcher.group(1);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract OTP from Mailpit: {}", e.getMessage());
        }
        return null;
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
