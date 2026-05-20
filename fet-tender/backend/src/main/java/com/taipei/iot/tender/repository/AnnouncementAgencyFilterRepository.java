package com.taipei.iot.tender.repository;

import com.taipei.iot.tender.entity.AnnouncementAgencyFilter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementAgencyFilterRepository extends JpaRepository<AnnouncementAgencyFilter, Long> {

    List<AnnouncementAgencyFilter> findByIsActiveTrueOrderBySolutionAscAgencyKeywordAsc();

    List<AnnouncementAgencyFilter> findBySolutionAndIsActiveTrue(String solution);
}
