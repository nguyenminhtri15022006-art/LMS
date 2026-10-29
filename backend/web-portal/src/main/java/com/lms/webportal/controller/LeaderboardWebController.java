package com.lms.webportal.controller;

import com.lms.webportal.dto.LeaderboardEntryDto;
import com.lms.webportal.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class LeaderboardWebController {

    @Autowired
    private LeaderboardService leaderboardService;

    @GetMapping("/leaderboard")
    public String showLeaderboard(@RequestParam(value = "period", required = false, defaultValue = "ALL_TIME") String period,
                                  Model model) {
        List<LeaderboardEntryDto> leaderboard = leaderboardService.getLeaderboard(period);

        LeaderboardEntryDto top1 = leaderboard.size() > 0 ? leaderboard.get(0) : null;
        LeaderboardEntryDto top2 = leaderboard.size() > 1 ? leaderboard.get(1) : null;
        LeaderboardEntryDto top3 = leaderboard.size() > 2 ? leaderboard.get(2) : null;

        model.addAttribute("leaderboard", leaderboard);
        model.addAttribute("top1", top1);
        model.addAttribute("top2", top2);
        model.addAttribute("top3", top3);
        model.addAttribute("period", period);
        model.addAttribute("pageTitle", "Bảng Xếp Hạng Học Tập - EduLearn LMS");
        return "leaderboard/index";
    }
}
