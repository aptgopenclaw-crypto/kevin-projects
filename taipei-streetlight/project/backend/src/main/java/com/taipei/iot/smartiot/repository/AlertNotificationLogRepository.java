package com.taipei.iot.smartiot.repository;

import com.taipei.iot.smartiot.entity.AlertNotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertNotificationLogRepository extends JpaRepository<AlertNotificationLog, Long> {

    List<AlertNotificationLog> findByAlertId(Long alertId);
}
