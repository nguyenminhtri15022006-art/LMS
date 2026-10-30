package com.lms.webportal.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ProfileUpdateForm {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 3, max = 50, message = "Họ và tên phải từ 3 đến 50 ký tự")
    @Pattern(regexp = "^[A-Za-zÀ-ỹ\\s]+$", message = "Họ và tên chỉ được chứa chữ cái và khoảng trắng")
    private String fullname;

    @Pattern(regexp = "^(0[0-9]{9})?$", message = "Số điện thoại phải gồm 10 chữ số bắt đầu bằng 0")
    private String phone;

    private String dob;

    private String bio;

    private String avatarUrl;

    private List<String> discardedAvatarUrls;

    public ProfileUpdateForm() {
    }

    public ProfileUpdateForm(String fullname, String phone, String dob, String bio, String avatarUrl) {
        this.fullname = fullname;
        this.phone = phone;
        this.dob = dob;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public List<String> getDiscardedAvatarUrls() {
        return discardedAvatarUrls;
    }

    public void setDiscardedAvatarUrls(List<String> discardedAvatarUrls) {
        this.discardedAvatarUrls = discardedAvatarUrls;
    }
}
