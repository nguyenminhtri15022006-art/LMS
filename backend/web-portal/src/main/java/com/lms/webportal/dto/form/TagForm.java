package com.lms.webportal.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TagForm {

    @NotBlank(message = "Tên thẻ tag không được để trống")
    @Size(max = 50, message = "Tên thẻ tag tối đa 50 ký tự")
    private String name;

    @NotBlank(message = "Đường dẫn slug không được để trống")
    @Size(max = 50, message = "Slug tối đa 50 ký tự")
    private String slug;

    public TagForm() {
    }

    public TagForm(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
