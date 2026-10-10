package com.xpense.service;

import com.xpense.dto.ProfileUpdateRequest;
import com.xpense.exception.BadRequestException;
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

    public UserProfile getProfile(String id) {
        return userProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found with id: " + id));
    }

    /**
     * Locks the user's profile row for the rest of the current transaction. Every action that
     * moves money calls this first, so two requests from the same user can't both pass the
     * "is there enough money" checks at the same time.
     */
    public UserProfile lockForMoneyChange(String id) {
        return userProfileRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found."));
    }

    /**
     * Updates only the details that were sent. Balance, email, role and account type
     * are never changed here: the balance only moves through money actions.
     */
    @Transactional
    public UserProfile updateProfile(String id, ProfileUpdateRequest update) {
        UserProfile profile = getProfile(id);

        if (update.getFullName() != null) {
            profile.setFullName(Inputs.requiredText(update.getFullName(), 100, "Your name", "Your name can't be empty."));
        }
        if (update.getStudentId() != null) profile.setStudentId(Inputs.text(update.getStudentId(), 50, "The roll number"));
        if (update.getUniversity() != null) profile.setUniversity(Inputs.text(update.getUniversity(), 120, "The college name"));
        if (update.getSemester() != null) profile.setSemester(Inputs.text(update.getSemester(), 50, "The semester"));
        if (update.getAvatarUrl() != null) profile.setAvatarUrl(Inputs.text(update.getAvatarUrl(), 255, "The picture link"));
        if (update.getBusinessName() != null) {
            profile.setBusinessName(Inputs.text(update.getBusinessName(), 120, "The business name"));
        }
        if (update.getTaxReservePercent() != null) {
            BigDecimal pct = update.getTaxReservePercent();
            if (pct.compareTo(BigDecimal.ZERO) < 0 || pct.compareTo(new BigDecimal("60")) > 0) {
                throw new BadRequestException("Tax to set aside must be between 0% and 60%.");
            }
            profile.setTaxReservePercent(pct);
        }

        return userProfileRepository.save(profile);
    }

    @Transactional
    public UserProfile adjustBalance(String id, BigDecimal delta) {
        UserProfile profile = getProfile(id);

        BigDecimal current = profile.getTotalBalance() != null ? profile.getTotalBalance() : BigDecimal.ZERO;
        BigDecimal updated = current.add(delta);
        if (updated.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("You don't have enough money for this.");
        }
        profile.setTotalBalance(updated);
        return userProfileRepository.save(profile);
    }
}
