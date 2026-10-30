package com.lms.webapp.service;

import com.lms.webapp.client.LeaderboardClient;
import com.lms.webapp.dto.ApiResponse;
import com.lms.webapp.dto.LeaderboardEntryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardService.class);

    @Autowired(required = false)
    private LeaderboardClient leaderboardClient;

    public List<LeaderboardEntryDto> getLeaderboard(String period) {
        if (leaderboardClient != null) {
            try {
                ApiResponse<List<LeaderboardEntryDto>> response = leaderboardClient.getLeaderboard(20);
                if (response != null && response.getResult() != null && !response.getResult().isEmpty()) {
                    return response.getResult();
                }
            } catch (Exception e) {
                log.warn("Không kết nối được LeaderboardClient, sử dụng dữ liệu vinh danh mặc định: {}", e.getMessage());
            }
        }

        return getDefaultLeaderboard();
    }

    private List<LeaderboardEntryDto> getDefaultLeaderboard() {
        return List.of(
                LeaderboardEntryDto.builder()
                        .rank(1)
                        .userId("usr-student-01")
                        .username("minhtri")
                        .fullname("Nguyễn Minh Trí")
                        .problemsSolved(25)
                        .quizAverage(9.6)
                        .totalScore(2450)
                        .badge("GOLD")
                        .build(),
                LeaderboardEntryDto.builder()
                        .rank(2)
                        .userId("usr-student-02")
                        .username("hoangcuong")
                        .fullname("Lê Hoàng Cường")
                        .problemsSolved(22)
                        .quizAverage(9.4)
                        .totalScore(2180)
                        .badge("SILVER")
                        .build(),
                LeaderboardEntryDto.builder()
                        .rank(3)
                        .userId("usr-student-03")
                        .username("quocbao")
                        .fullname("Trần Quốc Bảo")
                        .problemsSolved(20)
                        .quizAverage(9.1)
                        .totalScore(1950)
                        .badge("BRONZE")
                        .build(),
                LeaderboardEntryDto.builder()
                        .rank(4)
                        .userId("usr-student-04")
                        .username("thuthao")
                        .fullname("Phạm Thị Thu Thảo")
                        .problemsSolved(18)
                        .quizAverage(8.8)
                        .totalScore(1720)
                        .badge("TOP_CODER")
                        .build(),
                LeaderboardEntryDto.builder()
                        .rank(5)
                        .userId("usr-student-05")
                        .username("hoangnam")
                        .fullname("Vũ Hoàng Nam")
                        .problemsSolved(16)
                        .quizAverage(8.5)
                        .totalScore(1540)
                        .badge("TOP_CODER")
                        .build(),
                LeaderboardEntryDto.builder()
                        .rank(6)
                        .userId("usr-student-06")
                        .username("minhquan")
                        .fullname("Đỗ Minh Quân")
                        .problemsSolved(15)
                        .quizAverage(8.2)
                        .totalScore(1410)
                        .badge("TOP_CODER")
                        .build()
        );
    }
}
