package com.blog.cms.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByRefreshToken(String refreshToken);

    @Modifying
    @Query("UPDATE Session s SET s.revokedAt = :now WHERE s.refreshToken = :token")
    void revokeByToken(@Param("token") String refreshToken, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE Session s SET s.revokedAt = :now WHERE s.user.id = :userId AND s.revokedAt IS NULL")
    void revokeAllByUserId(@Param("userId") Long userId, @Param("now") Instant now);
}
