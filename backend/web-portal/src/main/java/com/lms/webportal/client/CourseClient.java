package com.lms.webportal.client;

import com.lms.webportal.dto.ApiResponse;
import com.lms.webportal.dto.CourseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "course-client", url = "${app.gateway.url:http://localhost:8080}")
public interface CourseClient {

    @GetMapping("/api/v1/courses")
    ApiResponse<List<CourseDto>> getCourses(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "difficulty", required = false) String difficulty
    );

    @GetMapping("/api/v1/courses/{id}")
    ApiResponse<CourseDto> getCourseById(@PathVariable("id") String id);
}
