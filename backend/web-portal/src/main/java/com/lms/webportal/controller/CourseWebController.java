package com.lms.webportal.controller;

import com.lms.webportal.dto.ChapterDto;
import com.lms.webportal.dto.CourseDto;
import com.lms.webportal.dto.CourseFilterDto;
import com.lms.webportal.dto.LessonDto;
import com.lms.webportal.dto.UserSessionDto;
import com.lms.webportal.service.CourseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class CourseWebController {

    @Autowired
    private CourseService courseService;

    @GetMapping("/courses")
    public String listCourses(@ModelAttribute("filter") CourseFilterDto filter,
                              Model model) {
        List<CourseDto> courses = courseService.getCourses(filter);
        List<String> categories = courseService.getAllCategories();

        model.addAttribute("courses", courses);
        model.addAttribute("categories", categories);
        model.addAttribute("pageTitle", "Khám Phá Khóa Học - EduLearn LMS");
        return "courses/list";
    }

    @GetMapping("/courses/{id}")
    public String showCourseDetail(@PathVariable("id") String id,
                                   @RequestParam(value = "enrolled", required = false) Boolean enrolled,
                                   Model model) {
        CourseDto course = courseService.getCourseById(id);
        if (course == null) {
            return "redirect:/courses";
        }

        if (Boolean.TRUE.equals(enrolled)) {
            model.addAttribute("success", "Chúc mừng bạn đã đăng ký tham gia khóa học thành công!");
        }

        model.addAttribute("course", course);
        model.addAttribute("pageTitle", course.getTitle() + " - EduLearn LMS");
        return "courses/detail";
    }

    @PostMapping("/courses/{id}/enroll")
    public String enrollCourse(@PathVariable("id") String id,
                               HttpSession session) {
        UserSessionDto user = (UserSessionDto) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/auth/login?redirectUrl=/courses/" + id;
        }

        courseService.enroll(id, user.getUserId());
        return "redirect:/courses/" + id + "?enrolled=true";
    }

    @GetMapping("/courses/{id}/lessons/{lessonId}")
    public String viewLesson(@PathVariable("id") String id,
                             @PathVariable("lessonId") String lessonId,
                             Model model) {
        CourseDto course = courseService.getCourseById(id);
        if (course == null) {
            return "redirect:/courses";
        }

        LessonDto currentLesson = courseService.getLessonById(id, lessonId);
        if (currentLesson == null) {
            // Lấy bài học đầu tiên nếu không tìm thấy bài chỉ định
            if (course.getChapters() != null && !course.getChapters().isEmpty()) {
                ChapterDto firstChapter = course.getChapters().get(0);
                if (firstChapter.getLessons() != null && !firstChapter.getLessons().isEmpty()) {
                    currentLesson = firstChapter.getLessons().get(0);
                }
            }
        }

        if (currentLesson == null) {
            return "redirect:/courses/" + id;
        }

        // Tìm bài học trước và sau để hỗ trợ điều hướng
        List<LessonDto> flatLessons = new ArrayList<>();
        if (course.getChapters() != null) {
            for (ChapterDto chapter : course.getChapters()) {
                if (chapter.getLessons() != null) {
                    flatLessons.addAll(chapter.getLessons());
                }
            }
        }

        LessonDto prevLesson = null;
        LessonDto nextLesson = null;
        for (int i = 0; i < flatLessons.size(); i++) {
            if (flatLessons.get(i).getId().equals(currentLesson.getId())) {
                if (i > 0) prevLesson = flatLessons.get(i - 1);
                if (i < flatLessons.size() - 1) nextLesson = flatLessons.get(i + 1);
                break;
            }
        }

        model.addAttribute("course", course);
        model.addAttribute("currentLesson", currentLesson);
        model.addAttribute("prevLesson", prevLesson);
        model.addAttribute("nextLesson", nextLesson);
        model.addAttribute("pageTitle", currentLesson.getTitle() + " - " + course.getTitle());
        return "courses/lesson";
    }
}
