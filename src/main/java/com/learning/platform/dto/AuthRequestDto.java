package com.learning.platform.dto;

public class AuthRequestDto {
    private String email;
    private String password;
    private String name;
    private String currentRoleTitle = "Associate Engineer";
    private String targetRoleId = "ROLE_CLOUD_ARCHITECT";

    public AuthRequestDto() {}

    public AuthRequestDto(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCurrentRoleTitle() { return currentRoleTitle; }
    public void setCurrentRoleTitle(String currentRoleTitle) { this.currentRoleTitle = currentRoleTitle; }
    public String getTargetRoleId() { return targetRoleId; }
    public void setTargetRoleId(String targetRoleId) { this.targetRoleId = targetRoleId; }
}
