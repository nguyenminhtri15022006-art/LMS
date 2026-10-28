package com.lms.webapp.deployment;

import com.lms.webapp.fixture.DockerStackReadinessVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm tra sức khỏe toàn bộ 11 container Docker Compose thật của Thành viên 2.
 * Được kích hoạt qua Maven Profile:
 * mvn test -pl web-app -f backend/pom.xml -Pdocker-smoke -Dtest=DockerStackReadinessIntegrationTest
 */
@Tag("docker-smoke")
class DockerStackReadinessIntegrationTest {

    @Test
    @DisplayName("Kiểm chứng toàn bộ 11 container trong stack Thành viên 2 đều đang chạy ở trạng thái Healthy / UP")
    void verifyLiveDockerComposeStackIsAllUp() {
        DockerStackReadinessVerifier verifier = new DockerStackReadinessVerifier();
        int mysqlPort = Integer.parseInt(System.getProperty("smoke.db.port", "3307"));

        List<DockerStackReadinessVerifier.ServiceEndpoint> stack =
                DockerStackReadinessVerifier.standardMember2Stack(mysqlPort);

        DockerStackReadinessVerifier.DockerReadinessReport report =
                verifier.verifyStack(stack, Duration.ofSeconds(5));

        System.out.println("=== KẾT QUẢ KIỂM TRA SỨC KHỎE DOCKER STACK THÀNH VIÊN 2 ===");
        report.getStatuses().forEach((name, status) -> {
            System.out.printf("[%s] %s -> %s (latency: %dms)%n",
                    status.isUp() ? "PASS" : "FAIL",
                    name,
                    status.message(),
                    status.latencyMs());
        });

        if (!report.isAllUp()) {
            System.err.println("Các dịch vụ chưa sẵn sàng: " + report.getDownServices());
        }

        assertThat(report.getTotalServices()).isEqualTo(11);
        assertThat(report.getUpCount())
                .as("Tất cả 11 container phải ở trạng thái UP. Dịch vụ lỗi: %s", report.getDownServices())
                .isEqualTo(11);
        assertThat(report.isAllUp()).isTrue();
    }
}

