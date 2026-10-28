package com.lms.webapp.fixture;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Đóng gói quy trình kiểm thử khả năng chịu lỗi (Fault-Injection Testing) tuân thủ quy tắc an toàn nghiêm ngặt:
 * 1. BƯỚC PHỤC HỒI BẮT BUỘC NẰM TRONG KHỐI FINALLY: Bất kể assertion kiểm thử PASS hay FAIL (ném Exception hoặc AssertionError),
 *    thao tác phục hồi (ví dụ docker compose start service) luôn luôn được kích hoạt.
 * 2. KIỂM TRA SỨC KHỎE (HEALTH-CHECK) BẮT BUỘC: Sau khi phục hồi, liên tục thăm dò health-check endpoint
 *    cho đến khi service thực sự trở lại trạng thái healthy/UP trước khi kết thúc test.
 * 3. TUYỆT ĐỐI KHÔNG TUYÊN BỐ "ĐÃ KHÔI PHỤC" NẾU HEALTH-CHECK THẤT BẠI HOẶC TIMEOUT: Ném lỗi ngoại lệ nghiêm trọng.
 * 4. BẢO TOÀN LỖI TEST BAN ĐẦU: Khi cả test và phục hồi đều thất bại, lỗi ban đầu của test luôn là lỗi chính (primary),
 *    lỗi phục hồi được gắn vào danh sách Suppressed Exception.
 */
public class FaultRecoverySafetyWrapper {

    public static class FaultTestContext {
        private final List<String> logs = new ArrayList<>();

        public void log(String message) {
            logs.add("[" + System.currentTimeMillis() + "] " + message);
        }

        public List<String> getLogs() {
            return Collections.unmodifiableList(logs);
        }
    }

    @FunctionalInterface
    public interface ThrowableConsumer<T> {
        void accept(T t) throws Throwable;
    }

    /**
     * Thực thi kịch bản kiểm thử lỗi có bảo hộ an toàn
     *
     * @param serviceName Tên dịch vụ bị thử thách lỗi
     * @param testAction Hành động thử nghiệm lỗi (ngắt kết nối, gọi API kiểm tra fallback/503)
     * @param recoveryAction Hành động phục hồi dịch vụ (bắt buộc chạy trong finally)
     * @param healthCheck Hàm kiểm tra sức khỏe của dịch vụ sau khi phục hồi
     * @param recoveryTimeout Thời gian tối đa chờ đợi dịch vụ phục hồi hoàn toàn
     * @param pollInterval Chu kỳ thăm dò giữa các lần kiểm tra
     */
    public static void executeWithSafeRecovery(
            String serviceName,
            ThrowableConsumer<FaultTestContext> testAction,
            Runnable recoveryAction,
            Supplier<Boolean> healthCheck,
            Duration recoveryTimeout,
            Duration pollInterval
    ) throws Throwable {
        FaultTestContext context = new FaultTestContext();
        context.log("Bắt đầu kịch bản Fault Testing cho service: " + serviceName);
        Throwable testError = null;

        try {
            if (testAction != null) {
                testAction.accept(context);
            }
        } catch (Throwable t) {
            context.log("Kịch bản kiểm thử ném ngoại lệ: " + t.getMessage());
            testError = t;
        } finally {
            context.log("Bắt đầu kích hoạt hành động phục hồi cho service: " + serviceName);
            Throwable recoveryError = null;

            if (recoveryAction != null) {
                try {
                    recoveryAction.run();
                    context.log("Hành động phục hồi đã được kích hoạt.");
                } catch (Throwable t) {
                    recoveryError = new IllegalStateException(
                            "CRITICAL FAULT RECOVERY ERROR: Khởi động lại service " + serviceName + " thất bại: " + t.getMessage(), t);
                }
            }

            // Kiểm tra sức khỏe (Health Check) bắt buộc
            boolean healthy = false;
            long deadline = System.currentTimeMillis() + Math.max(100, recoveryTimeout.toMillis());
            long intervalMs = Math.max(20, pollInterval != null ? pollInterval.toMillis() : 100);

            while (System.currentTimeMillis() < deadline) {
                try {
                    if (healthCheck != null && Boolean.TRUE.equals(healthCheck.get())) {
                        healthy = true;
                        context.log("Health check service " + serviceName + " thành công (UP).");
                        break;
                    }
                } catch (Throwable ignored) {
                    // Chờ chu kỳ tiếp theo
                }
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (!healthy && recoveryError == null) {
                recoveryError = new IllegalStateException(
                        "CRITICAL FAULT SAFETY FAILURE: Service " + serviceName + " không thể phục hồi về trạng thái Healthy trong "
                                + recoveryTimeout.toMillis() + "ms. Tuyệt đối không tuyên bố đã phục hồi!");
            }

            // Xử lý ném ngoại lệ tuân thủ quy tắc bảo toàn lỗi:
            // 1. Nếu test lỗi và phục hồi cũng lỗi: giữ testError làm lỗi chính, gắn recoveryError vào suppressed.
            // 2. Nếu test lỗi nhưng phục hồi thành công: ném testError.
            // 3. Nếu test thành công nhưng phục hồi/health check thất bại: ném recoveryError.
            if (testError != null) {
                if (recoveryError != null) {
                    testError.addSuppressed(recoveryError);
                }
                throw testError;
            }

            if (recoveryError != null) {
                throw recoveryError;
            }
        }
    }
}
