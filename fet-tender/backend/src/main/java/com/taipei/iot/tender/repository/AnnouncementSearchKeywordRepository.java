package com.taipei.iot.tender.repository;

import com.taipei.iot.tender.entity.AnnouncementSearchKeyword;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AnnouncementSearchKeywordRepository extends JpaRepository<AnnouncementSearchKeyword, Long>, TenantScopedRepository {

    List<AnnouncementSearchKeyword> findByIsActiveTrueOrderBySolutionAscKeywordAsc();

    List<AnnouncementSearchKeyword> findBySolutionAndIsActiveTrueOrderByKeywordAsc(String solution);

    List<AnnouncementSearchKeyword> findBySolutionOrderByKeywordAsc(String solution);

    @Query("SELECT DISTINCT k.solution FROM AnnouncementSearchKeyword k WHERE k.solution IS NOT NULL ORDER BY k.solution")
    List<String> findDistinctSolutions();
}
