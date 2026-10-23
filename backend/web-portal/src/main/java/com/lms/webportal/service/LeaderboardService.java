package com.lms.webportal.service;

import com.lms.webportal.dto.LeaderboardEntryDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaderboardService {

    public List<LeaderboardEntryDto> getLeaderboard(String period) {
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
