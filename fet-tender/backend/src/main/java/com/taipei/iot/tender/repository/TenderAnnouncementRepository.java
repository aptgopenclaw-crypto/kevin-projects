package com.taipei.iot.tender.repository;

import com.taipei.iot.tender.entity.TenderAnnouncement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TenderAnnouncementRepository extends JpaRepository<TenderAnnouncement, Long> {

    Optional<TenderAnnouncement> findBySolutionAndMatchedKeywordAndTenderNumberAndAnnouncementDate(
            String solution, String matchedKeyword, String tenderNumber, LocalDate announcementDate);

    @Query("""
        SELECT t FROM TenderAnnouncement t
        WHERE (:solution IS NULL OR t.solution = :solution)
          AND (:keyword  IS NULL OR t.matchedKeyword LIKE %:keyword%)
          AND (:agency   IS NULL OR t.agencyName    LIKE %:agency%)
          AND (:name     IS NULL OR t.tenderName    LIKE %:name%)
          AND (:dateFrom IS NULL OR t.announcementDate >= :dateFrom)
          AND (:dateTo   IS NULL OR t.announcementDate <= :dateTo)
        ORDER BY t.announcementDate DESC, t.id DESC
    """)
    Page<TenderAnnouncement> search(
            @Param("solution") String solution,
            @Param("keyword")  String keyword,
            @Param("agency")   String agency,
            @Param("name")     String name,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo")   LocalDate dateTo,
            Pageable pageable);

    // ── AI Function Calling 專用查詢 ──────────────────────────────────────────

    @Query("""
        SELECT t FROM TenderAnnouncement t
        WHERE (:solution         IS NULL OR t.solution         = :solution)
          AND (:tenderName       IS NULL OR t.tenderName       LIKE %:tenderName%)
          AND (:agencyName       IS NULL OR t.agencyName       LIKE %:agencyName%)
          AND (:dateFrom         IS NULL OR t.announcementDate >= :dateFrom)
          AND (:dateTo           IS NULL OR t.announcementDate <= :dateTo)
          AND (:procurementType  IS NULL OR t.procurementType  LIKE %:procurementType%)
          AND (:tenderMethod     IS NULL OR t.tenderMethod     LIKE %:tenderMethod%)
        ORDER BY t.announcementDate DESC, t.id DESC
    """)
    Page<TenderAnnouncement> searchByAi(
            @Param("solution")        String solution,
            @Param("tenderName")      String tenderName,
            @Param("agencyName")      String agencyName,
            @Param("dateFrom")        LocalDate dateFrom,
            @Param("dateTo")          LocalDate dateTo,
            @Param("procurementType") String procurementType,
            @Param("tenderMethod")    String tenderMethod,
            Pageable pageable);

    @Query("""
        SELECT t FROM TenderAnnouncement t
        WHERE t.announcementDate >= :fromDate
        ORDER BY t.announcementDate DESC, t.id DESC
    """)
    List<TenderAnnouncement> findRecentTenders(
            @Param("fromDate") LocalDate fromDate,
            Pageable pageable);

    @Query("""
        SELECT t FROM TenderAnnouncement t
        WHERE (:minBudget IS NULL OR t.budgetAmount >= :minBudget)
          AND (:maxBudget IS NULL OR t.budgetAmount <= :maxBudget)
          AND t.budgetAmount IS NOT NULL
        ORDER BY t.budgetAmount DESC
    """)
    Page<TenderAnnouncement> findByBudgetRange(
            @Param("minBudget") BigDecimal minBudget,
            @Param("maxBudget") BigDecimal maxBudget,
            Pageable pageable);

    @Query("""
        SELECT t.solution, COUNT(t), SUM(t.budgetAmount), AVG(t.budgetAmount)
        FROM TenderAnnouncement t
        WHERE (:dateFrom IS NULL OR t.announcementDate >= :dateFrom)
          AND (:dateTo   IS NULL OR t.announcementDate <= :dateTo)
        GROUP BY t.solution
        ORDER BY COUNT(t) DESC
    """)
    List<Object[]> getStatsBySolution(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo")   LocalDate dateTo);
}
