package com.taipei.iot.tender.repository;

import com.taipei.iot.tender.entity.AnnouncementAgencyFilter;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AnnouncementAgencyFilterRepository extends JpaRepository<AnnouncementAgencyFilter, Long>, TenantScopedRepository {

    List<AnnouncementAgencyFilter> findByIsActiveTrueOrderBySolutionAscAgencyKeywordAsc();

    List<AnnouncementAgencyFilter> findBySolutionAndIsActiveTrue(String solution);

    List<AnnouncementAgencyFilter> findBySolutionOrderByAgencyKeywordAsc(String solution);

    @Query("SELECT DISTINCT f.solution FROM AnnouncementAgencyFilter f WHERE f.solution IS NOT NULL ORDER BY f.solution")
    List<String> findDistinctSolutions();
}
