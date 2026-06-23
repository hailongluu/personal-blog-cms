package com.blog.cms.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findByEmailWithRole(@Param("email") String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findByIdWithRole(@Param("id") Long id);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :timestamp WHERE u.id = :id")
    void updateLastLoginAt(@Param("id") Long id, @Param("timestamp") Instant timestamp);
}
