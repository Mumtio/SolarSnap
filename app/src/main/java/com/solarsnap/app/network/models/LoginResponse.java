package com.solarsnap.app.network.models;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    private boolean success;
    private String message;
    
    @SerializedName("token")
    private String accessToken;
    
    @SerializedName("refreshToken")
    private String refreshToken;
    
    private UserInfo user;
    
    public static class UserInfo {
        @SerializedName("userId")
        private String userId;
        private String email;
        @SerializedName("fullName")
        private String fullName;
        private String role;
        @SerializedName("companyId")
        private String companyId;
        
        // Getters
        public String getUserId() { return userId; }
        public String getEmail() { return email; }
        public String getFullName() { return fullName; }
        public String getRole() { return role; }
        public String getCompanyId() { return companyId; }
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public UserInfo getUser() { return user; }
}
