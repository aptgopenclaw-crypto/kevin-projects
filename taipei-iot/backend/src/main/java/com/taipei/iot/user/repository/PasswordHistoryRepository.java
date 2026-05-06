package com.taipei.iot.user.repository;

import com.taipei.iot.user.entity.PasswordHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistoryEntity, Long> {
    List<PasswordHistoryEntity> findTop5ByUserIdOrderByCreateTimeDesc(String userId);
}
