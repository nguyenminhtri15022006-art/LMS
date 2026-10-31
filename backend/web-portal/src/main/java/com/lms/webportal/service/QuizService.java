package com.lms.webportal.service;

import com.lms.webportal.client.QuizClient;
import com.lms.webportal.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    @Autowired(required = false)
    private QuizClient quizClient;

    private final Map<Integer, QuizDto> quizRepository = new ConcurrentHashMap<>();
    private final List<QuizQuestionDto> globalQuestionBank = new ArrayList<>();
    private final List<QuizResultDto> submissionHistory = Collections.synchronizedList(new ArrayList<>());

    public QuizService() {
        initDefaultQuizzes();
    }

    private void initDefaultQuizzes() {
        // Quiz 1 Questions
        QuizQuestionDto q1 = QuizQuestionDto.builder()
                .questionId(101)
                .content("Trong kiến trúc Spring Cloud, thành phần nào chịu trách nhiệm đăng ký và khám phá dịch vụ (Service Discovery)?")
                .category("Microservices")
                .difficulty("EASY")
                .score(2)
                .options(List.of(
                        new QuizOptionDto("A", "Spring Cloud Gateway"),
                        new QuizOptionDto("B", "Netflix Eureka Server"),
                        new QuizOptionDto("C", "Spring Boot Actuator"),
                        new QuizOptionDto("D", "Resilience4j")
                ))
                .correctAnswer("B")
                .explanation("Netflix Eureka Server đóng vai trò Service Registry, cho phép các microservices đăng ký IP/Port và tự động phát hiện nhau mà không cần cấu hình cứng địa chỉ.")
                .build();

        QuizQuestionDto q2 = QuizQuestionDto.builder()
                .questionId(102)
                .content("Mục đích cốt lõi của kỹ thuật 'Token Offloading' tại API Gateway là gì?")
                .category("Microservices")
                .difficulty("MEDIUM")
                .score(2)
                .options(List.of(
                        new QuizOptionDto("A", "Lưu trữ mật khẩu người dùng vào Session"),
                        new QuizOptionDto("B", "Xác thực JWT tập trung tại Gateway và chuyển tiếp User ID qua HTTP Headers cho các dịch vụ bên dưới"),
                        new QuizOptionDto("C", "Mã hóa toàn bộ cơ sở dữ liệu MySQL"),
                        new QuizOptionDto("D", "Chặn đứng mọi request từ các IP nước ngoài")
                ))
                .correctAnswer("B")
                .explanation("Token Offloading giúp giải phóng các downstream services khỏi việc phải xác thực chữ ký JWT nhiều lần; Gateway xác thực một lần và truyền X-User-Id, X-User-Role vào request downstream.")
                .build();

        QuizQuestionDto q3 = QuizQuestionDto.builder()
                .questionId(103)
                .content("Khi một microservice gặp sự cố kéo dài, mẫu thiết kế (design pattern) nào giúp ngăn ngừa lỗi dây chuyền (Cascading Failure)?")
                .category("Microservices")
                .difficulty("MEDIUM")
                .score(2)
                .options(List.of(
                        new QuizOptionDto("A", "Singleton Pattern"),
                        new QuizOptionDto("B", "Circuit Breaker Pattern"),
                        new QuizOptionDto("C", "Observer Pattern"),
                        new QuizOptionDto("D", "Factory Method Pattern")
                ))
                .correctAnswer("B")
                .explanation("Circuit Breaker (Ngắt mạch) tự động ngắt kết nối khi tỷ lệ lỗi vượt ngưỡng, chuyển sang trả về phản hồi fallback và bảo vệ các dịch vụ khác.")
                .build();

        QuizQuestionDto q4 = QuizQuestionDto.builder()
                .questionId(104)
                .content("Spring Cloud Gateway được xây dựng dựa trên nền tảng lập trình phản ứng (Reactive) nào?")
                .category("Microservices")
                .difficulty("HARD")
                .score(2)
                .options(List.of(
                        new QuizOptionDto("A", "Spring WebFlux & Project Reactor (Netty)"),
                        new QuizOptionDto("B", "Apache Tomcat & Servlet Blocking API"),
                        new QuizOptionDto("C", "Node.js Express"),
                        new QuizOptionDto("D", "JAX-RS Jersey")
                ))
                .correctAnswer("A")
                .explanation("Spring Cloud Gateway sử dụng Spring WebFlux và Netty không chặn (non-blocking), đem lại hiệu năng xử lý hàng chục ngàn kết nối đồng thời với lượng tài nguyên tối thiểu.")
                .build();

        QuizQuestionDto q5 = QuizQuestionDto.builder()
                .questionId(105)
                .content("Đặc điểm nào sau đây KHÔNG PHẢI là đặc trưng của giao thức HTTP RESTful API chuẩn?")
                .category("Web Development")
                .difficulty("EASY")
                .score(2)
                .options(List.of(
                        new QuizOptionDto("A", "Stateless (Phi trạng thái)"),
                        new QuizOptionDto("B", "Sử dụng các phương thức chuẩn GET, POST, PUT, DELETE"),
                        new QuizOptionDto("C", "Máy chủ lưu trữ trạng thái người dùng trong Session trên RAM máy chủ"),
                        new QuizOptionDto("D", "Biểu diễn tài nguyên dạng JSON hoặc XML")
                ))
                .correctAnswer("C")
                .explanation("Kiến trúc RESTful bắt buộc phải Stateless, máy chủ không lưu giữ session state của client giữa các request.")
                .build();

        globalQuestionBank.addAll(List.of(q1, q2, q3, q4, q5));

        // Quiz 1
        QuizDto quiz1 = QuizDto.builder()
                .quizId(1)
                .courseId("c2222222-2222-2222-2222-222222222222")
                .title("Kiến Trúc Microservices & Spring Cloud Gateway")
                .description("Bài kiểm tra đánh giá mức độ hiểu biết về Service Discovery, Token Offloading, Circuit Breaker và kiến trúc Non-blocking.")
                .totalScore(10)
                .passScore(5)
                .maxAttempts(3)
                .durationMinutes(15)
                .questionCount(5)
                .category("Microservices")
                .difficulty("MEDIUM")
                .completed(true)
                .bestScore(9.0)
                .questions(List.of(q1, q2, q3, q4, q5))
                .build();

        // Quiz 2
        QuizDto quiz2 = QuizDto.builder()
                .quizId(2)
                .courseId("c1111111-1111-1111-1111-111111111111")
                .title("Lập Trình Web Nâng Cao Với Spring Boot 3 & REST API")
                .description("Đánh giá kiến thức về Dependency Injection, Bean Lifecycle, Spring Data JPA và xử lý ngoại lệ Global Exception.")
                .totalScore(10)
                .passScore(5)
                .maxAttempts(2)
                .durationMinutes(20)
                .questionCount(5)
                .category("Web Development")
                .difficulty("EASY")
                .completed(false)
                .questions(List.of(q1, q2, q5))
                .build();

        // Quiz 3
        QuizDto quiz3 = QuizDto.builder()
                .quizId(3)
                .courseId("c1111111-1111-1111-1111-111111111111")
                .title("Cấu Trúc Dữ Liệu & Độ Phức Tạp Thuật Toán")
                .description("Kiểm tra kiến thức về Big-O Notation, Hash Table, Binary Search Tree, Heap và Graph Algorithms.")
                .totalScore(10)
                .passScore(6)
                .maxAttempts(2)
                .durationMinutes(25)
                .questionCount(5)
                .category("Algorithms")
                .difficulty("HARD")
                .completed(false)
                .questions(List.of(q3, q4, q5))
                .build();

        quizRepository.put(quiz1.getQuizId(), quiz1);
        quizRepository.put(quiz2.getQuizId(), quiz2);
        quizRepository.put(quiz3.getQuizId(), quiz3);
    }

    public List<QuizDto> getQuizzes(String category, String difficulty) {
        if (quizClient != null) {
            try {
                ApiResponse<List<QuizDto>> res = quizClient.getQuizzes(category, difficulty);
                if (res != null && res.getResult() != null && !res.getResult().isEmpty()) {
                    return res.getResult();
                }
            } catch (Exception e) {
                log.warn("Không thể kết nối QuizClient, dùng dữ liệu mẫu: {}", e.getMessage());
            }
        }

        return quizRepository.values().stream()
                .filter(q -> {
                    if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
                        if (!category.equalsIgnoreCase(q.getCategory())) return false;
                    }
                    if (difficulty != null && !difficulty.isBlank() && !"ALL".equalsIgnoreCase(difficulty)) {
                        if (!difficulty.equalsIgnoreCase(q.getDifficulty())) return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(QuizDto::getQuizId))
                .collect(Collectors.toList());
    }

    public QuizDto getQuizById(Integer id) {
        if (quizClient != null) {
            try {
                ApiResponse<QuizDto> res = quizClient.getQuizById(id);
                if (res != null && res.getResult() != null) {
                    return res.getResult();
                }
            } catch (Exception e) {
                log.warn("Lỗi gọi QuizClient getQuizById: {}", e.getMessage());
            }
        }
        return quizRepository.get(id);
    }

    public QuizResultDto submitQuiz(QuizSubmitDto submitDto, String userId) {
        QuizDto quiz = getQuizById(submitDto.getQuizId());
        if (quiz == null) {
            throw new IllegalArgumentException("Không tìm thấy bài thi Quiz ID: " + submitDto.getQuizId());
        }

        List<QuizQuestionDto> questions = quiz.getQuestions();
        int totalQuestions = questions.size();
        int correctCount = 0;
        List<QuestionResultDto> details = new ArrayList<>();

        for (QuizQuestionDto q : questions) {
            String selected = submitDto.getAnswers() != null ? submitDto.getAnswers().get(q.getQuestionId()) : null;
            boolean isCorrect = selected != null && selected.equalsIgnoreCase(q.getCorrectAnswer());
            if (isCorrect) {
                correctCount++;
            }

            details.add(QuestionResultDto.builder()
                    .questionId(q.getQuestionId())
                    .content(q.getContent())
                    .selectedOption(selected != null ? selected : "Chưa chọn")
                    .correctOption(q.getCorrectAnswer())
                    .isCorrect(isCorrect)
                    .explanation(q.getExplanation())
                    .options(q.getOptions())
                    .build());
        }

        double score = totalQuestions > 0 ? ((double) correctCount / totalQuestions) * 10.0 : 0.0;
        boolean passed = score >= (quiz.getPassScore() != null ? quiz.getPassScore() : 5.0);
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        quiz.setCompleted(true);
        if (quiz.getBestScore() == null || score > quiz.getBestScore()) {
            quiz.setBestScore(score);
        }

        QuizResultDto res = QuizResultDto.builder()
                .quizId(quiz.getQuizId())
                .quizTitle(quiz.getTitle())
                .totalQuestions(totalQuestions)
                .correctCount(correctCount)
                .score(Math.round(score * 10.0) / 10.0)
                .passed(passed)
                .timeSpentSeconds(submitDto.getTimeSpentSeconds() != null ? submitDto.getTimeSpentSeconds() : 180)
                .submittedAt(now)
                .questionResults(details)
                .build();
        submissionHistory.add(0, res);
        return res;
    }

    public List<QuizQuestionDto> getQuestionBank(String category) {
        return globalQuestionBank.stream()
                .filter(q -> category == null || category.isBlank() || "ALL".equalsIgnoreCase(category) || category.equalsIgnoreCase(q.getCategory()))
                .collect(Collectors.toList());
    }

    public List<QuizResultDto> getSubmissionsByUser(String userId) {
        if (submissionHistory.isEmpty()) {
            QuizResultDto initial = QuizResultDto.builder()
                    .quizId(1)
                    .quizTitle("Trắc nghiệm Kiến trúc Microservices & Spring Cloud")
                    .totalQuestions(5)
                    .correctCount(5)
                    .score(10.0)
                    .passed(true)
                    .timeSpentSeconds(180)
                    .submittedAt("2026-10-31 09:33:00")
                    .build();
            submissionHistory.add(initial);
        }
        return new ArrayList<>(submissionHistory);
    }
}
