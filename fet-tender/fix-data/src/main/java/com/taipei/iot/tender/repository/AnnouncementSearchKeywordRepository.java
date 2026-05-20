package com.taipei.iot.tender.repository;

import com.taipei.iot.tender.entity.AnnouncementSearchKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementSearchKeywordRepository extends JpaRepository<AnnouncementSearchKeyword, Long> {

    List<AnnouncementSearchKeyword> findByIsActiveTrueOrderBySolutionAscKeywordAsc();
}
