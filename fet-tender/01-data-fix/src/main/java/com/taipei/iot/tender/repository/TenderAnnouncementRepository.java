package com.taipei.iot.tender.repository;

import com.taipei.iot.tender.entity.TenderAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TenderAnnouncementRepository extends JpaRepository<TenderAnnouncement, Long> {

    Optional<TenderAnnouncement> findByTenantIdAndSolutionAndMatchedKeywordAndTenderNumberAndAnnouncementDate(
            String tenantId, String solution, String matchedKeyword, String tenderNumber, LocalDate announcementDate);
}
