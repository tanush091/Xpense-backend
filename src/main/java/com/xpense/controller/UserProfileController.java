package com.xpense.controller;

import com.xpense.dto.ApiResponse;
import com.xpense.dto.ProfileUpdateRequest;
import com.xpense.model.UserProfile;
import com.xpense.security.SecurityUtils;
import com.xpense.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfile>> getCurrentProfile() {
        String effectiveUserId = SecurityUtils.getAuthenticatedUserId();
        UserProfile profile = userProfileService.getProfile(effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserProfile>> updateProfile(@RequestBody ProfileUpdateRequest updateData) {
        String effectiveUserId = SecurityUtils.getAuthenticatedUserId();
        UserProfile updated = userProfileService.updateProfile(effectiveUserId, updateData);
        return ResponseEntity.ok(ApiResponse.success("Settings saved", updated));
    }
}
