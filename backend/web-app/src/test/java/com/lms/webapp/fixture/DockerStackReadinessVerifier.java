package com.lms.webapp.fixture;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Xác thực sức khỏe và tính sẵn sàng của các dịch vụ trong stack Docker Compose của Thành viên 2.
 * Hỗ trợ kiểm tra qua giao thức HTTP (Actuator health / API endpoints) và TCP port ping (CSDL / Broker).
 */
public class DockerStackReadinessVerifier {

    public record ServiceEndpoint(String name, String host, int port, String healthPath, boolean isHttp) {
        public static ServiceEndpoint http(String name, String host, int port, String healthPath) {
            return new ServiceEndpoint(name, host, port, healthPath, true);
        }

        public static ServiceEndpoint tcp(String name, String host, int port) {
            return new ServiceEndpoint(name, host, port, null, false);
        }

        public String getUrl() {
            if (!isHttp) {
                return host + ":" + port;
            }
            String path = healthPath != null ? healthPath : "";
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            return "http://" + host + ":" + port + path;
        }
    }

    public record ServiceHealthStatus(
            String name,
            String endpoint,
            boolean isUp,
            int statusCode,
            long latencyMs,
            String message
    ) {}

    public static class DockerReadinessReport {
        private final Map<String, ServiceHealthStatus> statuses = new LinkedHashMap<>();

        public void addStatus(ServiceHealthStatus status) {
            statuses.put(status.name(), status);
        }

        public Map<String, ServiceHealthStatus> getStatuses() {
            return Collections.unmodifiableMap(statuses);
        }

        public boolean isAllUp() {
            return !statuses.isEmpty() && statuses.values().stream().allMatch(ServiceHealthStatus::isUp);
        }

        public List<String> getDownServices() {
            return statuses.values().stream()
                    .filter(s -> !s.isUp())
                    .map(s -> s.name() + " (" + s.endpoint() + ": " + s.message() + ")")
                    .toList();
        }

        public int getTotalServices() {
            return statuses.size();
        }

        public int getUpCount() {
            return (int) statuses.values().stream().filter(ServiceHealthStatus::isUp).count();
        }
    }

    /**
     * Danh sách 11 dịch vụ tiêu chuẩn thuộc stack Thành viên 2 theo docker-compose.yml
     */
    public static List<ServiceEndpoint> standardMember2Stack(int mysqlHostPort) {
        List<ServiceEndpoint> list = new ArrayList<>();
        // 1. Hạ tầng CSDL & Lưu trữ
        list.add(ServiceEndpoint.tcp("mysql", "127.0.0.1", mysqlHostPort > 0 ? mysqlHostPort : 3307));
        list.add(ServiceEndpoint.tcp("mongodb", "127.0.0.1", 27017));
        list.add(ServiceEndpoint.tcp("redis", "127.0.0.1", 6379));
        list.add(ServiceEndpoint.http("minio", "127.0.0.1", 9000, "/minio/health/live"));
        list.add(ServiceEndpoint.http("mailpit", "127.0.0.1", 8025, "/api/v1/messages"));

        // 2. Hạ tầng Microservices
        list.add(ServiceEndpoint.http("service-registry", "127.0.0.1", 8761, "/eureka/apps"));
        list.add(ServiceEndpoint.http("api-gateway", "127.0.0.1", 8080, "/actuator/health"));
        list.add(ServiceEndpoint.http("identity-service", "127.0.0.1", 8081, "/identity/actuator/health"));
        list.add(ServiceEndpoint.http("course-service", "127.0.0.1", 8082, "/course/actuator/health"));
        list.add(ServiceEndpoint.http("blog-service", "127.0.0.1", 8084, "/blog/actuator/health"));

        // 3. Web App BFF
        list.add(ServiceEndpoint.http("web-app", "127.0.0.1", 8088, "/actuator/health"));

        return list;
    }

    public DockerReadinessReport verifyStack(List<ServiceEndpoint> endpoints, Duration timeout) {
        DockerReadinessReport report = new DockerReadinessReport();
        int timeoutMs = (int) Math.max(100, timeout.toMillis());
        for (ServiceEndpoint endpoint : endpoints) {
            report.addStatus(checkEndpoint(endpoint, timeoutMs));
        }
        return report;
    }

    public ServiceHealthStatus checkEndpoint(ServiceEndpoint endpoint, int timeoutMs) {
        long start = System.currentTimeMillis();
        if (endpoint.isHttp()) {
            return checkHttp(endpoint.name(), endpoint.getUrl(), timeoutMs, start);
        } else {
            return checkTcp(endpoint.name(), endpoint.host(), endpoint.port(), timeoutMs, start);
        }
    }

    public ServiceHealthStatus checkHttp(String name, String urlString, int timeoutMs, long startTime) {
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(urlString).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setInstanceFollowRedirects(false);

            int code = conn.getResponseCode();
            long latency = System.currentTimeMillis() - startTime;
            // 200..299 hoặc 302 (chuyển hướng login) được xem là service đang sống
            boolean isUp = (code >= 200 && code < 400);
            String msg = isUp ? "OK (HTTP " + code + ")" : "HTTP " + code;
            return new ServiceHealthStatus(name, urlString, isUp, code, latency, msg);
        } catch (IOException e) {
            long latency = System.currentTimeMillis() - startTime;
            return new ServiceHealthStatus(name, urlString, false, -1, latency, e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public ServiceHealthStatus checkTcp(String name, String host, int port, int timeoutMs, long startTime) {
        String endpoint = host + ":" + port;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            long latency = System.currentTimeMillis() - startTime;
            return new ServiceHealthStatus(name, endpoint, true, 200, latency, "Connected");
        } catch (IOException e) {
            long latency = System.currentTimeMillis() - startTime;
            return new ServiceHealthStatus(name, endpoint, false, -1, latency, "Connection failed: " + e.getMessage());
        }
    }
}
