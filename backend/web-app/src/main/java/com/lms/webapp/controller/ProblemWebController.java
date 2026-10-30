package com.lms.webapp.controller;

import com.lms.webapp.dto.CodeSubmitDto;
import com.lms.webapp.dto.ProblemDto;
import com.lms.webapp.dto.ProblemFilterDto;
import com.lms.webapp.dto.SubmissionResultDto;
import com.lms.webapp.dto.UserSessionDto;
import com.lms.webapp.service.ProblemService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class ProblemWebController {

    @Autowired
    private ProblemService problemService;

    @GetMapping("/problems")
    public String listProblems(@ModelAttribute("filter") ProblemFilterDto filter,
                               Model model) {
        List<ProblemDto> problems = problemService.getProblems(filter);

        long solvedCount = problems.stream().filter(p -> Boolean.TRUE.equals(p.getCompleted())).count();

        model.addAttribute("problems", problems);
        model.addAttribute("totalProblems", problems.size());
        model.addAttribute("solvedCount", solvedCount);
        model.addAttribute("pageTitle", "Luyện Tập Lập Trình & Thuật Toán - EduLearn LMS");
        return "problems/list";
    }

    @GetMapping("/problems/{id}")
    public String showSolveWorkspace(@PathVariable("id") Integer id,
                                     @RequestParam(value = "lang", required = false, defaultValue = "CPP") String lang,
                                     HttpSession session,
                                     Model model) {
        ProblemDto problem = problemService.getProblemById(id);
        if (problem == null) {
            return "redirect:/problems";
        }

        UserSessionDto user = (UserSessionDto) session.getAttribute("currentUser");
        List<SubmissionResultDto> submissions = problemService.getSubmissionsByProblem(id, user != null ? user.getUserId() : null);

        if (!model.containsAttribute("codeSubmitDto")) {
            CodeSubmitDto submitDto = new CodeSubmitDto();
            submitDto.setProblemId(id);
            submitDto.setLanguage(lang);

            if ("JAVA".equalsIgnoreCase(lang)) {
                submitDto.setSourceCode(problem.getDefaultTemplateJava());
            } else if ("PYTHON".equalsIgnoreCase(lang)) {
                submitDto.setSourceCode(problem.getDefaultTemplatePython());
            } else {
                submitDto.setSourceCode(problem.getDefaultTemplateCpp());
            }

            model.addAttribute("codeSubmitDto", submitDto);
        }

        model.addAttribute("problem", problem);
        model.addAttribute("submissions", submissions);
        model.addAttribute("selectedLang", lang);
        model.addAttribute("pageTitle", problem.getTitle() + " - EduLearn Code Judge");
        return "problems/solve";
    }

    @PostMapping("/problems/{id}/submit")
    public String submitCode(@PathVariable("id") Integer id,
                             @ModelAttribute("codeSubmitDto") CodeSubmitDto codeSubmitDto,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        UserSessionDto user = (UserSessionDto) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/auth/login?redirectUrl=/problems/" + id;
        }

        codeSubmitDto.setProblemId(id);
        SubmissionResultDto result = problemService.submitCode(codeSubmitDto, user.getUserId());

        redirectAttributes.addFlashAttribute("lastResult", result);
        redirectAttributes.addFlashAttribute("codeSubmitDto", codeSubmitDto);
        return "redirect:/problems/" + id;
    }
}
