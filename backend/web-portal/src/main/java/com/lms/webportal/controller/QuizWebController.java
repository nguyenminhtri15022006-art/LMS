package com.lms.webportal.controller;

import com.lms.webportal.dto.*;
import com.lms.webportal.service.QuizService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class QuizWebController {

    @Autowired
    private QuizService quizService;

    @GetMapping("/quizzes")
    public String listQuizzes(@RequestParam(value = "category", required = false) String category,
                              @RequestParam(value = "difficulty", required = false) String difficulty,
                              Model model) {
        List<QuizDto> quizzes = quizService.getQuizzes(category, difficulty);

        model.addAttribute("quizzes", quizzes);
        model.addAttribute("selectedCategory", category != null ? category : "ALL");
        model.addAttribute("selectedDifficulty", difficulty != null ? difficulty : "ALL");
        model.addAttribute("pageTitle", "Thi Trắc Nghiệm Trực Tuyến - EduLearn LMS");
        return "quizzes/list";
    }

    @GetMapping("/quizzes/{id}")
    public String showQuizOverview(@PathVariable("id") Integer id, Model model) {
        QuizDto quiz = quizService.getQuizById(id);
        if (quiz == null) {
            return "redirect:/quizzes";
        }

        model.addAttribute("quiz", quiz);
        model.addAttribute("pageTitle", quiz.getTitle() + " - Quy chế phòng thi");
        return "quizzes/overview";
    }

    @GetMapping("/quizzes/{id}/take")
    public String enterExamRoom(@PathVariable("id") Integer id,
                                HttpSession session,
                                Model model) {
        UserSessionDto user = (UserSessionDto) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/auth/login?redirectUrl=/quizzes/" + id + "/take";
        }

        QuizDto quiz = quizService.getQuizById(id);
        if (quiz == null) {
            return "redirect:/quizzes";
        }

        QuizSubmitDto submitDto = new QuizSubmitDto();
        submitDto.setQuizId(id);

        model.addAttribute("quiz", quiz);
        model.addAttribute("quizSubmitDto", submitDto);
        model.addAttribute("pageTitle", "Phòng Thi: " + quiz.getTitle());
        return "quizzes/take";
    }

    @PostMapping("/quizzes/{id}/submit")
    public String submitExam(@PathVariable("id") Integer id,
                             @ModelAttribute("quizSubmitDto") QuizSubmitDto submitDto,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        UserSessionDto user = (UserSessionDto) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/auth/login?redirectUrl=/quizzes/" + id + "/take";
        }

        submitDto.setQuizId(id);
        QuizResultDto result = quizService.submitQuiz(submitDto, user.getUserId());

        redirectAttributes.addFlashAttribute("quizResult", result);
        return "redirect:/quizzes/" + id + "/result";
    }

    @GetMapping("/quizzes/{id}/result")
    public String showQuizResult(@PathVariable("id") Integer id,
                                 Model model) {
        QuizDto quiz = quizService.getQuizById(id);
        if (quiz == null) {
            return "redirect:/quizzes";
        }

        if (!model.containsAttribute("quizResult")) {
            // Mẫu kết quả mặc định nếu vào trực tiếp
            QuizSubmitDto dummy = new QuizSubmitDto();
            dummy.setQuizId(id);
            QuizResultDto result = quizService.submitQuiz(dummy, "demo-user");
            model.addAttribute("quizResult", result);
        }

        model.addAttribute("quiz", quiz);
        model.addAttribute("pageTitle", "Kết Quả Thi: " + quiz.getTitle());
        return "quizzes/result";
    }

    @GetMapping("/questions")
    public String showQuestionBank(@RequestParam(value = "category", required = false) String category,
                                   Model model) {
        List<QuizQuestionDto> questions = quizService.getQuestionBank(category);

        model.addAttribute("questions", questions);
        model.addAttribute("selectedCategory", category != null ? category : "ALL");
        model.addAttribute("pageTitle", "Ngân Hàng Câu Hỏi Trắc Nghiệm - EduLearn");
        return "questions/bank";
    }
}
