package com.xpense.service;

import com.xpense.exception.ResourceNotFoundException;
import com.xpense.model.UserProfile;
import com.xpense.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class UserProfileService {

    public static final String DEFAULT_USER_ID = "user-default-1";

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public UserProfile getDefaultProfile() {
        return userProfileRepository.findById(DEFAULT_USER_ID)
                .orElseGet(() -> {
                    UserProfile profile = new UserProfile(
                            DEFAULT_USER_ID,
                            "128003008@sastra.ac.in",
                            "Aditya Venkata Sai Burle",
                            "128003008@sastra.ac.in",
                            new BigDecimal("2450.00")
                    );
                    return userProfileRepository.save(profile);
                });
    }

    public UserProfile getProfile(String id) {
        return userProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found with id: " + id));
    }

    public UserProfile getProfileOrDefault(String id) {
        if (id == null || id.isEmpty()) {
            return getDefaultProfile();
        }
        return userProfileRepository.findById(id).orElseGet(this::getDefaultProfile);
    }

    @Transactional
    public UserProfile updateProfile(String id, UserProfile updateData) {
        UserProfile profile = userProfileRepository.findById(id)
                .orElseGet(this::getDefaultProfile);

        if (updateData.getFullName() != null && !updateData.getFullName().trim().isEmpty()) {
            profile.setFullName(updateData.getFullName().trim());
        }
        if (updateData.getEmail() != null && !updateData.getEmail().trim().isEmpty()) {
            profile.setEmail(updateData.getEmail().trim());
        }
        if (updateData.getStudentId() != null && !updateData.getStudentId().trim().isEmpty()) {
            profile.setStudentId(updateData.getStudentId().trim());
        }
        if (updateData.getUniversity() != null && !updateData.getUniversity().trim().isEmpty()) {
            profile.setUniversity(updateData.getUniversity().trim());
        }
        if (updateData.getSemester() != null && !updateData.getSemester().trim().isEmpty()) {
            profile.setSemester(updateData.getSemester().trim());
        }
        if (updateData.getAvatarUrl() != null) {
            profile.setAvatarUrl(updateData.getAvatarUrl());
        }
        if (updateData.getRole() != null) {
            profile.setRole(updateData.getRole());
        }
        // accountType is immutable per profile once created; do not overwrite if existing
        if (profile.getAccountType() == null || profile.getAccountType().isEmpty()) {
            if (updateData.getAccountType() != null && !updateData.getAccountType().trim().isEmpty()) {
                profile.setAccountType(updateData.getAccountType().trim());
            }
        }
        if (updateData.getCurrency() != null && !updateData.getCurrency().trim().isEmpty()) {
            profile.setCurrency(updateData.getCurrency().trim());
        }
        if (updateData.getCurrencySymbol() != null && !updateData.getCurrencySymbol().trim().isEmpty()) {
            profile.setCurrencySymbol(updateData.getCurrencySymbol().trim());
        }
        if (updateData.getTotalBalance() != null) {
            profile.setTotalBalance(updateData.getTotalBalance());
        }

        return userProfileRepository.save(profile);
    }

    @Transactional
    public UserProfile adjustBalance(String id, BigDecimal delta) {
        UserProfile profile = userProfileRepository.findById(id)
                .orElseGet(this::getDefaultProfile);

        BigDecimal current = profile.getTotalBalance() != null ? profile.getTotalBalance() : BigDecimal.ZERO;
        BigDecimal updated = current.add(delta);
        if (updated.compareTo(BigDecimal.ZERO) < 0) {
            updated = BigDecimal.ZERO;
        }
        profile.setTotalBalance(updated);
        return userProfileRepository.save(profile);
    }
}
