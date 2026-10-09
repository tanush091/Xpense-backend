package com.xpense.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @JsonProperty("full_name")
    @JsonAlias({"fullName", "full_name", "name"})
    private String fullName;

    @JsonProperty("account_type")
    @JsonAlias({"accountType", "account_type"})
    private String accountType = "Student Account";

    public RegisterRequest() {
    }

    public RegisterRequest(String email, String password, String fullName, String accountType) {
        setEmail(email);
        this.password = password;
        setFullName(fullName);
        this.accountType = accountType != null ? accountType : "Student Account";
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim() : null;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @JsonProperty("full_name")
    public String getFullName() {
        return fullName;
    }

    @JsonProperty("full_name")
    @JsonAlias({"fullName", "full_name", "name"})
    public void setFullName(String fullName) {
        this.fullName = fullName != null ? fullName.trim() : null;
    }

    @JsonProperty("account_type")
    public String getAccountType() {
        return accountType;
    }

    @JsonProperty("account_type")
    @JsonAlias({"accountType", "account_type"})
    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
}
