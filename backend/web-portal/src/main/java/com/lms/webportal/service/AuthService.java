package com.lms.webportal.service;

import com.lms.webportal.client.AuthClient;
import com.lms.webportal.dto.ApiResponse;
import com.lms.webportal.dto.LoginDto;
import com.lms.webportal.dto.RegisterDto;
import com.lms.webportal.dto.UserSessionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class AuthService {

    @Autowired(required = false)
    private AuthClient authClient;

    public UserSessionDto authenticate(LoginDto loginDto) {
        if (loginDto.getUsername() == null || loginDto.getUsername().isBlank()
                || loginDto.getPassword() == null || loginDto.getPassword().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
        }

        if (authClient == null) {
            throw new IllegalStateException("Không tìm thấy kết nối tới Identity Service.");
        }

        try {
            ApiResponse<Map<String, Object>> response = authClient.login(Map.of(
                    "username", loginDto.getUsername().trim(),
                    "password", loginDto.getPassword()
            ));

            if (response != null && response.getResult() != null) {
                Map<String, Object> result = response.getResult();
                String accessToken = (String) result.get("accessToken");
                @SuppressWarnings("unchecked")
                Map<String, Object> user = (Map<String, Object>) result.get("user");

                String userId = user != null ? (String) (user.get("userId") != null ? user.get("userId") : user.get("id")) : null;
                String fullname = user != null && user.get("fullname") != null ? (String) user.get("fullname") : loginDto.getUsername();
                String email = user != null && user.get("email") != null ? (String) user.get("email") : "";
                String role = user != null && user.get("role") != null ? (String) user.get("role") : "STUDENT";

                return UserSessionDto.builder()
                        .userId(userId)
                        .username(loginDto.getUsername().trim())
                        .fullname(fullname)
                        .email(email)
                        .role(role)
                        .accessToken(accessToken)
                        .build();
            }
            throw new IllegalArgumentException("Phản hồi không hợp lệ từ máy chủ xác thực.");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Xác thực thất bại với Identity Service: {}", e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("chưa được kích hoạt") || msg.contains("1009")) {
                throw new IllegalArgumentException("Tài khoản chưa được kích hoạt qua email. Vui lòng kiểm tra mã OTP gửi về email!");
            }
            if (msg.contains("1005") || msg.contains("không chính xác") || msg.contains("401") || msg.contains("400")) {
                throw new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không chính xác.");
            }
            throw new IllegalArgumentException("Đăng nhập thất bại: Tên đăng nhập hoặc mật khẩu không đúng.");
        }
    }

    public void register(RegisterDto registerDto) {
        if (!registerDto.getPassword().equals(registerDto.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        }

        if (authClient == null) {
            throw new IllegalStateException("Không tìm thấy kết nối tới Identity Service.");
        }

        try {
            authClient.register(Map.of(
                    "username", registerDto.getUsername().trim(),
                    "password", registerDto.getPassword(),
                    "fullname", registerDto.getFullname(),
                    "email", registerDto.getEmail(),
                    "role", registerDto.getRole() != null ? registerDto.getRole() : "STUDENT"
            ));
        } catch (Exception e) {
            log.error("Đăng ký thất bại với Identity Service: {}", e.getMessage());
            throw new IllegalArgumentException("Đăng ký thất bại: " + e.getMessage());
        }
    }
}
