package com.lms.webportal.service;

import com.lms.webportal.client.ChatbotClient;
import com.lms.webportal.dto.ApiResponse;
import com.lms.webportal.dto.ChatRequestDto;
import com.lms.webportal.dto.ChatResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    @Autowired(required = false)
    private ChatbotClient chatbotClient;

    public ChatResponseDto askChatbot(ChatRequestDto request) {
        String sessionId = request.getSessionId() != null && !request.getSessionId().isBlank()
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        if (chatbotClient != null) {
            try {
                ApiResponse<ChatResponseDto> response = chatbotClient.sendMessage(request);
                if (response != null && response.getResult() != null) {
                    return response.getResult();
                }
            } catch (Exception e) {
                log.warn("Không kết nối được Chatbot Client Gateway, sử dụng trợ lý AI tích hợp nội bộ: {}", e.getMessage());
            }
        }

        // Logic trợ lý gia sư AI tích hợp nội bộ (Intelligent Educational Assistant)
        String p = request.getPrompt() != null ? request.getPrompt().toLowerCase().trim() : "";
        String reply;

        if (p.contains("two sum") || p.contains("tổng 2 số") || p.contains("hai số")) {
            reply = "Chào bạn! Đối với bài toán **Two Sum**, có 2 hướng giải quyết tối ưu:\n\n"
                    + "1. **Dùng Bảng băm (Hash Map)**: Duyệt mảng 1 lần, với mỗi phần tử `nums[i]`, kiểm tra xem `target - nums[i]` đã có trong Map chưa. Độ phức tạp: **O(N) thời gian** và **O(N) bộ nhớ**.\n"
                    + "2. **Kỹ thuật Hai con trỏ (Two Pointers)**: Nếu mảng đã sắp xếp, đặt 1 con trỏ ở đầu và 1 con trỏ ở cuối. Độ phức tạp: **O(N log N)** do sắp xếp.\n\n"
                    + "Bạn có thể vào tab **Luyện Code -> Bài 1 (Two Sum)** để nộp bài thử nghiệm ngay nhé!";
        } else if (p.contains("microservices") || p.contains("gateway") || p.contains("eureka")) {
            reply = "Hệ thống EduLearn LMS ứng dụng kiến trúc **Spring Cloud Microservices** gồm các thành phần trọng yếu:\n\n"
                    + "- **Service Registry (Eureka)**: Quản lý đăng ký và khám phá dịch vụ động trên cổng `8761`.\n"
                    + "- **API Gateway (Spring Cloud Gateway)**: Cổng vào duy nhất tại cổng `8080`, thực hiện **Token Offloading** (xác thực JWT tập trung, gán headers `X-User-Id`, `X-User-Role` cho các downstream services).\n"
                    + "- **Resilience4j Circuit Breaker**: Tự động ngắt mạch và trả về phản hồi fallback khi dịch vụ phía sau quá tải.";
        } else if (p.contains("dijkstra") || p.contains("đồ thị") || p.contains("ngắn nhất")) {
            reply = "Thuật toán **Dijkstra** tìm đường đi ngắn nhất từ 1 đỉnh nguồn tới các đỉnh còn lại trên đồ thị có trọng số không âm:\n\n"
                    + "- Cấu trúc dữ liệu tối ưu: Sử dụng **Min-Heap (Priority Queue)** để luôn lấy đỉnh có khoảng cách nhỏ nhất.\n"
                    + "- Độ phức tạp thời gian: `O((V + E) log V)`.\n"
                    + "- Lưu ý: Không áp dụng được cho đồ thị có cạnh mang trọng số âm (khi đó cần dùng Bellman-Ford).";
        } else if (p.contains("quiz") || p.contains("trắc nghiệm") || p.contains("thi")) {
            reply = "Hệ thống thi trắc nghiệm của EduLearn hỗ trợ phòng thi đếm ngược thời gian thực. Sau khi nộp bài, bạn sẽ nhận được điểm số /10 và xem được **lời giải thích chi tiết** cho từng câu hỏi để ôn tập hiệu quả nhất!";
        } else {
            reply = "Xin chào! Tôi là **Trợ lý AI EduLearn**. Tôi có thể hỗ trợ bạn:\n\n"
                    + "1. Gợi ý thuật toán và tối ưu độ phức tạp (Two Sum, Knapsack, Dijkstra...)\n"
                    + "2. Giải thích kiến trúc hệ thống Microservices & Spring Boot\n"
                    + "3. Hướng dẫn ôn thi các bộ đề trắc nghiệm Quiz\n\n"
                    + "Bạn cần giải đáp thắc mắc nào về bài học hay bài tập lập trình hôm nay?";
        }

        return ChatResponseDto.builder()
                .sessionId(sessionId)
                .reply(reply)
                .build();
    }
}
