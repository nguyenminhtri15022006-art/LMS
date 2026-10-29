package com.lms.courseservice.configuration;

import com.lms.courseservice.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssignmentClientFallbackTest {

    private AssignmentClient.Fallback fallback;

    @BeforeEach
    void setUp() {
        fallback = new AssignmentClient.Fallback();
    }

    @Test
    @DisplayName("1. assignmentClientFallbackReturnsZeroExerciseCountWhenInvoked: Fallback getExerciseCount trả về ApiResponse<Long> = 0L")
    void assignmentClientFallbackReturnsZeroExerciseCountWhenInvoked() {
        ApiResponse<Long> response = fallback.getExerciseCount("course-123");

        assertNotNull(response);
        assertNotNull(response.getData());
        assertEquals(0L, response.getData());
    }

    @Test
    @DisplayName("2. assignmentClientFallbackReturnsEmptyMapForBatchWhenInvoked: Fallback getExerciseCountBatch trả về ApiResponse<Map<String, Long>> = Map.of()")
    void assignmentClientFallbackReturnsEmptyMapForBatchWhenInvoked() {
        ApiResponse<Map<String, Long>> response = fallback.getExerciseCountBatch(List.of("course-1", "course-2"));

        assertNotNull(response);
        assertNotNull(response.getData());
        assertTrue(response.getData().isEmpty());
    }
}
