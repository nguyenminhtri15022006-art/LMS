package com.lms.webportal.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleForm {

    @NotBlank(message = "Tiêu đề lịch học không được để trống")
    @Size(max = 500, message = "Tiêu đề tối đa 500 ký tự")
    private String title;

    @NotBlank(message = "Thời gian bắt đầu không được để trống")
    private String startTime;

    @NotBlank(message = "Thời gian kết thúc không được để trống")
    private String endTime;

    private String note;
}
