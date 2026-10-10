package com.xpense.repository;

import com.xpense.model.UserProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    Optional<UserProfile> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<UserProfile> findByStudentId(String studentId);

    /** Locks the profile row until the surrounding transaction ends, so one user's money actions run one at a time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from UserProfile p where p.id = :id")
    Optional<UserProfile> findByIdForUpdate(@Param("id") String id);
}
