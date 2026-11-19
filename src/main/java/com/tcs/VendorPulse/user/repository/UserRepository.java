package com.tcs.VendorPulse.user.repository;

import com.tcs.VendorPulse.common.enums.AccountStatus;
import com.tcs.VendorPulse.common.enums.UserRole;
import com.tcs.VendorPulse.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByUserCode(String userCode);
    Optional<User> findByEmailIgnoreCaseOrUserCode(String email, String userCode);
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findBySupplierId(UUID supplierId);

    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:q IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<User> searchUsers(
            @Param("role") UserRole role,
            @Param("status") AccountStatus status,
            @Param("q") String q,
            Pageable pageable);
}

