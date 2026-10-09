package com.xpense.repository;

import com.xpense.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    Optional<UserProfile> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<UserProfile> findByStudentId(String studentId);
}
