package com.lms.webapp.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LockCourseForm {

    @NotBlank(message = "Lý do khóa khóa học không được để trống")
    @Size(max = 1000, message = "Lý do khóa tối đa 1000 ký tự")
    private String reason;

    public LockCourseForm() {
    }

    public LockCourseForm(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
