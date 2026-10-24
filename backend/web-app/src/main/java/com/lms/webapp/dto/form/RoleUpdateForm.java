package com.lms.webapp.dto.form;

import jakarta.validation.constraints.NotBlank;

public class RoleUpdateForm {

    @NotBlank(message = "Vai trò không được để trống")
    private String role;

    public RoleUpdateForm() {
    }

    public RoleUpdateForm(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
