package com.lms.webportal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("pageTitle", "EduLearn LMS - Nền Tảng Học Tập & Luyện Code Trực Tuyến");
        model.addAttribute("features", List.of(
                Map.of(
                        "icon", "bi-code-square",
                        "title", "Thực Hành Code Judge",
                        "desc", "Biên dịch và chấm điểm tự động đa ngôn ngữ (C++, Java, Python, Go) với Judge0 Engine.",
                        "badge", "Trọng tâm Đồ án",
                        "link", "/problems"
                ),
                Map.of(
                        "icon", "bi-patch-question",
                        "title", "Ngân Hàng Câu Hỏi & Quiz",
                        "desc", "Thi trắc nghiệm tính giờ, trộn câu hỏi, chấm điểm tự động và lưu lịch sử làm bài.",
                        "badge", "Tự động hóa",
                        "link", "/quizzes"
                ),
                Map.of(
                        "icon", "bi-journal-bookmark",
                        "title", "Khóa Học Trực Tuyến",
                        "desc", "Học tập theo lộ trình với video bài giảng, tài liệu đính kèm và theo dõi tiến độ.",
                        "badge", "Đa dạng",
                        "link", "/courses"
                ),
                Map.of(
                        "icon", "bi-robot",
                        "title", "Trợ Lý Học Tập AI",
                        "desc", "Chatbot AI hỗ trợ giải đáp thắc mắc lập trình và gợi ý giải bài tập 24/7.",
                        "badge", "AI Hỗ trợ",
                        "link", "/chatbot"
                )
        ));

        model.addAttribute("stats", List.of(
                Map.of("label", "Khóa học chất lượng", "value", "50+"),
                Map.of("label", "Bài tập lập trình", "value", "200+"),
                Map.of("label", "Học viên đăng ký", "value", "1,500+"),
                Map.of("label", "Lượt nộp bài chấm tự động", "value", "10,000+")
        ));

        return "home/index";
    }
}
