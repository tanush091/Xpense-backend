package com.xpense.dto;

import com.xpense.model.UserProfile;

public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private UserProfile user;

    public AuthResponse() {
    }

    public AuthResponse(String token, UserProfile user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public UserProfile getUser() {
        return user;
    }

    public void setUser(UserProfile user) {
        this.user = user;
    }
}
