package com.lms.webapp.controller;

import com.lms.webapp.dto.ChatRequestDto;
import com.lms.webapp.dto.ChatResponseDto;
import com.lms.webapp.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ChatWebController {

    @Autowired
    private ChatbotService chatbotService;

    @GetMapping("/chat")
    public String showChatPage(Model model) {
        model.addAttribute("pageTitle", "Trợ Lý AI Gia Sư Ảo - EduLearn LMS");
        return "chat/index";
    }

    @PostMapping("/api/portal/chat")
    @ResponseBody
    public ChatResponseDto processChat(@RequestBody ChatRequestDto request) {
        return chatbotService.askChatbot(request);
    }
}
