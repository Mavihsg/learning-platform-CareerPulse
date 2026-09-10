package com.learning.platform.dto;

public class AuthResponseDto {
    private boolean success;
    private String message;
    private String token;
    private UserProfileDto user;

    public AuthResponseDto() {}

    public AuthResponseDto(boolean success, String message, String token, UserProfileDto user) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.user = user;
    }

    public static AuthResponseDto success(String token, UserProfileDto user) {
        return new AuthResponseDto(true, "Authentication successful", token, user);
    }

    public static AuthResponseDto failure(String message) {
        return new AuthResponseDto(false, message, null, null);
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public UserProfileDto getUser() { return user; }
    public void setUser(UserProfileDto user) { this.user = user; }
}
