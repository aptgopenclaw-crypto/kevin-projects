package com.taipei.iot.auth.repository;

import com.taipei.iot.auth.entity.UserResetPasswordTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserResetPasswordTokenRepository extends JpaRepository<UserResetPasswordTokenEntity, String> {
    Optional<UserResetPasswordTokenEntity> findByToken(String token);
}
