package com.lms.webapp.client;

import com.lms.webapp.dto.ApiResponse;
import com.lms.webapp.dto.LeaderboardEntryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "leaderboard-client", url = "${app.gateway.url:http://localhost:8080}")
public interface LeaderboardClient {

    @GetMapping("/api/assignment/leaderboard")
    ApiResponse<List<LeaderboardEntryDto>> getLeaderboard(
            @RequestParam(value = "limit", defaultValue = "20") int limit
    );
}
