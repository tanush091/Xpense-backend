package com.xpense.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;

/**
 * Details a user may change about themselves. Missing fields are left as they are.
 * Balance, email, role and account type are deliberately not here.
 */
public class ProfileUpdateRequest {

    @JsonAlias({"fullName", "full_name", "name"})
    private String fullName;
    @JsonAlias({"studentId", "student_id"})
    private String studentId;
    private String university;
    private String semester;
    @JsonAlias({"avatarUrl", "avatar_url"})
    private String avatarUrl;
    @JsonAlias({"businessName", "business_name"})
    private String businessName;
    @JsonAlias({"taxReservePercent", "tax_reserve_percent"})
    private BigDecimal taxReservePercent;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public BigDecimal getTaxReservePercent() { return taxReservePercent; }
    public void setTaxReservePercent(BigDecimal taxReservePercent) { this.taxReservePercent = taxReservePercent; }
}
