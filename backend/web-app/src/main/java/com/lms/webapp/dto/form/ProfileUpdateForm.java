package com.lms.webapp.dto.form;

public class ProfileUpdateForm {

    private String fullName;
    private String phoneNumber;
    private String bio;
    private String avatarUrl;

    public ProfileUpdateForm() {
    }

    public ProfileUpdateForm(String fullName, String phoneNumber, String bio, String avatarUrl) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
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
}
