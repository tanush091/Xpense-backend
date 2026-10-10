package com.xpense.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "profiles")
public class UserProfile {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "full_name")
    @JsonProperty("full_name")
    @JsonAlias({"fullName", "full_name", "name"})
    private String fullName = "";

    @Column(name = "avatar_url")
    @JsonProperty("avatar_url")
    @JsonAlias({"avatarUrl", "avatar_url"})
    private String avatarUrl;

    @Column(name = "student_id")
    @JsonProperty("student_id")
    @JsonAlias({"studentId", "student_id"})
    private String studentId = "";

    @Column(name = "university")
    @JsonProperty("university")
    @JsonAlias({"university", "college", "institute"})
    private String university = "National Institute of Technology";

    @Column(name = "semester")
    @JsonProperty("semester")
    @JsonAlias({"semester", "year", "academicTerm"})
    private String semester = "Semester 4";

    @Column
    private String role = "user";

    @Column(name = "account_type")
    @JsonProperty("account_type")
    @JsonAlias({"accountType", "account_type"})
    private String accountType = "Student Account";

    @Column
    private String currency = "INR";

    @Column(name = "currency_symbol")
    @JsonProperty("currency_symbol")
    @JsonAlias({"currencySymbol", "currency_symbol"})
    private String currencySymbol = "₹";

    @Column(name = "total_balance", precision = 12, scale = 2)
    @JsonProperty("total_balance")
    @JsonAlias({"totalBalance", "total_balance"})
    private BigDecimal totalBalance = BigDecimal.ZERO;

    /** Business dashboard: company or trading name. */
    @Column(name = "business_name")
    @JsonProperty("business_name")
    @JsonAlias({"businessName", "business_name"})
    private String businessName;

    /** Business dashboard: share of money in to keep aside for tax, 0–60. */
    @Column(name = "tax_reserve_percent", precision = 5, scale = 2)
    @JsonProperty("tax_reserve_percent")
    @JsonAlias({"taxReservePercent", "tax_reserve_percent"})
    private BigDecimal taxReservePercent = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("created_at")
    @JsonAlias({"createdAt", "created_at"})
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    @JsonAlias({"updatedAt", "updated_at"})
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UserProfile() {
    }

    public UserProfile(String id, String email, String fullName, String studentId, BigDecimal totalBalance) {
        this.id = id;
        this.email = email;
        this.fullName = fullName != null ? fullName : "";
        this.studentId = studentId != null ? studentId : email;
        this.totalBalance = totalBalance != null ? totalBalance : BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @JsonProperty("full_name")
    public String getFullName() {
        return fullName;
    }

    @JsonProperty("fullName")
    public String getFullNameCamel() {
        return fullName;
    }

    @JsonProperty("name")
    public String getNameAlias() {
        return fullName;
    }

    @JsonProperty("full_name")
    @JsonAlias({"fullName", "full_name", "name"})
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @JsonProperty("avatar_url")
    public String getAvatarUrl() {
        return avatarUrl;
    }

    @JsonProperty("avatarUrl")
    public String getAvatarUrlCamel() {
        return avatarUrl;
    }

    @JsonProperty("avatar_url")
    @JsonAlias({"avatarUrl", "avatar_url"})
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    @JsonProperty("student_id")
    public String getStudentId() {
        return studentId;
    }

    @JsonProperty("studentId")
    public String getStudentIdCamel() {
        return studentId;
    }

    @JsonProperty("student_id")
    @JsonAlias({"studentId", "student_id"})
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    @JsonProperty("university")
    public String getUniversity() {
        return university;
    }

    @JsonProperty("university")
    @JsonAlias({"university", "college", "institute"})
    public void setUniversity(String university) {
        this.university = university;
    }

    @JsonProperty("semester")
    public String getSemester() {
        return semester;
    }

    @JsonProperty("semester")
    @JsonAlias({"semester", "year", "academicTerm"})
    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @JsonProperty("account_type")
    public String getAccountType() {
        return accountType;
    }

    @JsonProperty("accountType")
    public String getAccountTypeCamel() {
        return accountType;
    }

    @JsonProperty("account_type")
    @JsonAlias({"accountType", "account_type"})
    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @JsonProperty("currency_symbol")
    public String getCurrencySymbol() {
        return currencySymbol;
    }

    @JsonProperty("currencySymbol")
    public String getCurrencySymbolCamel() {
        return currencySymbol;
    }

    @JsonProperty("currency_symbol")
    @JsonAlias({"currencySymbol", "currency_symbol"})
    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    @JsonProperty("total_balance")
    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    @JsonProperty("totalBalance")
    public BigDecimal getTotalBalanceCamel() {
        return totalBalance;
    }

    @JsonProperty("total_balance")
    @JsonAlias({"totalBalance", "total_balance"})
    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    @JsonProperty("created_at")
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @JsonProperty("createdAt")
    public LocalDateTime getCreatedAtCamel() {
        return createdAt;
    }

    @JsonProperty("created_at")
    @JsonAlias({"createdAt", "created_at"})
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @JsonProperty("updated_at")
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @JsonProperty("updatedAt")
    public LocalDateTime getUpdatedAtCamel() {
        return updatedAt;
    }

    @JsonProperty("updated_at")
    @JsonAlias({"updatedAt", "updated_at"})
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @JsonProperty("business_name")
    public String getBusinessName() {
        return businessName;
    }

    @JsonProperty("business_name")
    @JsonAlias({"businessName", "business_name"})
    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    @JsonProperty("tax_reserve_percent")
    public BigDecimal getTaxReservePercent() {
        return taxReservePercent;
    }

    @JsonProperty("tax_reserve_percent")
    @JsonAlias({"taxReservePercent", "tax_reserve_percent"})
    public void setTaxReservePercent(BigDecimal taxReservePercent) {
        this.taxReservePercent = taxReservePercent;
    }
}
