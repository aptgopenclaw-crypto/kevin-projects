package com.taipei.iot.tender.repository;

import com.taipei.iot.tender.dto.VendorProjections;
import com.taipei.iot.tender.entity.TenderAward;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TenderAwardRepository extends JpaRepository<TenderAward, Long>, TenantScopedRepository {

    Optional<TenderAward> findBySolutionAndMatchedKeywordAndTenderNumberAndAwardAnnounceDateAndAwardAnnounceSeqAndVendorOrderSeq(
            String solution,
            String matchedKeyword,
            String tenderNumber,
            LocalDate awardAnnounceDate,
            String awardAnnounceSeq,
            Integer vendorOrderSeq);

    /**
     * 決標公告搜尋（分頁）。
     * 以 (tender_number, award_announce_date, award_announce_seq, vendor_order_seq) 為實體唯一鍵去重，
     * 避免同一標案被多個 matched_keyword 爬到而重複顯示。
     */
    @Query(
        nativeQuery = true,
        value = """
            WITH deduped AS (
                SELECT DISTINCT ON (tender_number, award_announce_date, award_announce_seq, vendor_order_seq)
                    *
                FROM tender_award
                WHERE tenant_id = :tenantId
                  AND (:solution   IS NULL OR solution       = :solution)
                  AND (:keyword    IS NULL OR matched_keyword ILIKE CONCAT('%', :keyword,    '%'))
                  AND (:agency     IS NULL OR agency_name     ILIKE CONCAT('%', :agency,     '%'))
                  AND (:name       IS NULL OR tender_name     ILIKE CONCAT('%', :name,       '%'))
                  AND (:vendorName IS NULL OR vendor_name     ILIKE CONCAT('%', :vendorName, '%'))
                  AND (CAST(:dateFrom AS DATE) IS NULL OR award_announce_date >= CAST(:dateFrom AS DATE))
                  AND (CAST(:dateTo   AS DATE) IS NULL OR award_announce_date <= CAST(:dateTo   AS DATE))
                ORDER BY tender_number, award_announce_date, award_announce_seq, vendor_order_seq
            )
            SELECT * FROM deduped
            ORDER BY award_announce_date DESC, tender_number ASC, vendor_order_seq ASC
        """,
        countQuery = """
            SELECT COUNT(*) FROM (
                SELECT DISTINCT ON (tender_number, award_announce_date, award_announce_seq, vendor_order_seq)
                    id
                FROM tender_award
                WHERE tenant_id = :tenantId
                  AND (:solution   IS NULL OR solution       = :solution)
                  AND (:keyword    IS NULL OR matched_keyword ILIKE CONCAT('%', :keyword,    '%'))
                  AND (:agency     IS NULL OR agency_name     ILIKE CONCAT('%', :agency,     '%'))
                  AND (:name       IS NULL OR tender_name     ILIKE CONCAT('%', :name,       '%'))
                  AND (:vendorName IS NULL OR vendor_name     ILIKE CONCAT('%', :vendorName, '%'))
                  AND (CAST(:dateFrom AS DATE) IS NULL OR award_announce_date >= CAST(:dateFrom AS DATE))
                  AND (CAST(:dateTo   AS DATE) IS NULL OR award_announce_date <= CAST(:dateTo   AS DATE))
                ORDER BY tender_number, award_announce_date, award_announce_seq, vendor_order_seq
            ) sub
        """
    )
    Page<TenderAward> search(
            @Param("tenantId")   String tenantId,
            @Param("solution")   String solution,
            @Param("keyword")    String keyword,
            @Param("agency")     String agency,
            @Param("name")       String name,
            @Param("vendorName") String vendorName,
            @Param("dateFrom")   String dateFrom,
            @Param("dateTo")     String dateTo,
            Pageable pageable);

    // ── 廠商 Dashboard 查詢 ───────────────────────────────────────────────────

    /**
     * 廠商名稱模糊搜尋建議，依得標件數降冪。
     * 搜尋輸入為空字串時回傳件數最多的前 20 筆廠商。
     */
    @Query(nativeQuery = true, value = """
        SELECT vendor_name,
               vendor_tax_id,
               COUNT(*) AS win_count
        FROM tender_award
        WHERE tenant_id = :tenantId
          AND vendor_name ILIKE CONCAT('%', :q, '%')
        GROUP BY vendor_name, vendor_tax_id
        ORDER BY win_count DESC
        LIMIT 20
    """)
    List<VendorProjections.Suggest> suggestVendors(
            @Param("tenantId") String tenantId,
            @Param("q") String q);

    /**
     * 取得廠商的最早與最晚決標日期（用於自動判斷趨勢時間粒度）。
     * 以 taxId 優先（精確），無 taxId 時退回 vendorName 完全相符。
     */
    @Query(nativeQuery = true, value = """
        SELECT MIN(award_announce_date) AS min_date,
               MAX(award_announce_date) AS max_date
        FROM tender_award
        WHERE tenant_id = :tenantId
          AND ((NULLIF(:taxId, '') IS NOT NULL AND vendor_tax_id = :taxId)
            OR (NULLIF(:taxId, '') IS NULL AND vendor_name = :vendorName))
    """)
    List<VendorProjections.DateRange> getVendorDateRange(
            @Param("tenantId") String tenantId,
            @Param("taxId") String taxId,
            @Param("vendorName") String vendorName);

    /** KPI 摘要（總件數、總金額、機關數、solution 數、首尾日期、代表名稱）。
     * deduped CTE 先去重，避免同標案被多個 matched_keyword 爬到而重複計算。
     */
    @Query(nativeQuery = true, value = """
        WITH deduped AS (
            SELECT DISTINCT ON (tender_number, award_announce_date, award_announce_seq, vendor_order_seq)
                agency_name, solution, award_announce_date, vendor_award_amount, vendor_name, vendor_tax_id
            FROM tender_award
            WHERE tenant_id = :tenantId
              AND ((NULLIF(:taxId, '') IS NOT NULL AND vendor_tax_id = :taxId)
                OR (NULLIF(:taxId, '') IS NULL AND vendor_name = :vendorName))
            ORDER BY tender_number, award_announce_date, award_announce_seq, vendor_order_seq
        )
        SELECT COUNT(*)                      AS total_wins,
               COALESCE(SUM(vendor_award_amount), 0) AS total_amount,
               COUNT(DISTINCT agency_name)   AS agency_count,
               COUNT(DISTINCT solution)      AS solution_count,
               MIN(award_announce_date)      AS first_award_date,
               MAX(award_announce_date)      AS latest_award_date,
               MAX(vendor_name)              AS vendor_name,
               MAX(vendor_tax_id)            AS vendor_tax_id
        FROM deduped
    """)
    List<VendorProjections.Overview> getVendorOverview(
            @Param("tenantId") String tenantId,
            @Param("taxId") String taxId,
            @Param("vendorName") String vendorName);

    /** 趨勢：日粒度（資料跨度 < 90 天時使用）。 */
    @Query(nativeQuery = true, value = """
        WITH deduped AS (
            SELECT DISTINCT ON (tender_number, award_announce_date, award_announce_seq, vendor_order_seq)
                award_announce_date, vendor_award_amount
            FROM tender_award
            WHERE tenant_id = :tenantId
              AND ((NULLIF(:taxId, '') IS NOT NULL AND vendor_tax_id = :taxId)
                OR (NULLIF(:taxId, '') IS NULL AND vendor_name = :vendorName))
            ORDER BY tender_number, award_announce_date, award_announce_seq, vendor_order_seq
        )
        SELECT TO_CHAR(award_announce_date, 'YYYY-MM-DD') AS period,
               COUNT(*) AS count,
               COALESCE(SUM(vendor_award_amount), 0) AS total_amount
        FROM deduped
        GROUP BY award_announce_date
        ORDER BY award_announce_date
    """)
    List<VendorProjections.Trend> trendByDay(
            @Param("tenantId") String tenantId,
            @Param("taxId") String taxId,
            @Param("vendorName") String vendorName);

    /** 趨勢：月粒度（資料跨度 90–729 天時使用）。 */
    @Query(nativeQuery = true, value = """
        WITH deduped AS (
            SELECT DISTINCT ON (tender_number, award_announce_date, award_announce_seq, vendor_order_seq)
                award_announce_date, vendor_award_amount
            FROM tender_award
            WHERE tenant_id = :tenantId
              AND ((NULLIF(:taxId, '') IS NOT NULL AND vendor_tax_id = :taxId)
                OR (NULLIF(:taxId, '') IS NULL AND vendor_name = :vendorName))
            ORDER BY tender_number, award_announce_date, award_announce_seq, vendor_order_seq
        )
        SELECT TO_CHAR(DATE_TRUNC('month', award_announce_date), 'YYYY-MM') AS period,
               COUNT(*) AS count,
               COALESCE(SUM(vendor_award_amount), 0) AS total_amount
        FROM deduped
        GROUP BY DATE_TRUNC('month', award_announce_date)
        ORDER BY DATE_TRUNC('month', award_announce_date)
    """)
    List<VendorProjections.Trend> trendByMonth(
            @Param("tenantId") String tenantId,
            @Param("taxId") String taxId,
            @Param("vendorName") String vendorName);

    /** 趨勢：季粒度（資料跨度 ≥ 730 天時使用）。 */
    @Query(nativeQuery = true, value = """
        WITH deduped AS (
            SELECT DISTINCT ON (tender_number, award_announce_date, award_announce_seq, vendor_order_seq)
                award_announce_date, vendor_award_amount
            FROM tender_award
            WHERE tenant_id = :tenantId
              AND ((NULLIF(:taxId, '') IS NOT NULL AND vendor_tax_id = :taxId)
                OR (NULLIF(:taxId, '') IS NULL AND vendor_name = :vendorName))
            ORDER BY tender_number, award_announce_date, award_announce_seq, vendor_order_seq
        )
        SELECT TO_CHAR(DATE_TRUNC('quarter', award_announce_date), 'YYYY-"Q"Q') AS period,
               COUNT(*) AS count,
               COALESCE(SUM(vendor_award_amount), 0) AS total_amount
        FROM deduped
        GROUP BY DATE_TRUNC('quarter', award_announce_date)
        ORDER BY DATE_TRUNC('quarter', award_announce_date)
    """)
    List<VendorProjections.Trend> trendByQuarter(
            @Param("tenantId") String tenantId,
            @Param("taxId") String taxId,
            @Param("vendorName") String vendorName);

    /** Solution × matched_keyword 金額與件數分布（用於 Treemap）。 */
    @Query(nativeQuery = true, value = """
        WITH deduped AS (
            SELECT DISTINCT ON (tender_number, award_announce_date, award_announce_seq, vendor_order_seq)
                solution, vendor_award_amount
            FROM tender_award
            WHERE tenant_id = :tenantId
              AND ((NULLIF(:taxId, '') IS NOT NULL AND vendor_tax_id = :taxId)
                OR (NULLIF(:taxId, '') IS NULL AND vendor_name = :vendorName))
            ORDER BY tender_number, award_announce_date, award_announce_seq, vendor_order_seq
        )
        SELECT solution,
               solution AS keyword,
               COUNT(*) AS count,
               COALESCE(SUM(vendor_award_amount), 0) AS total_amount
        FROM deduped
        GROUP BY solution
        ORDER BY total_amount DESC
    """)
    List<VendorProjections.SolutionRow> getSolutionBreakdown(
            @Param("tenantId") String tenantId,
            @Param("taxId") String taxId,
            @Param("vendorName") String vendorName);

    /** 得標機關排行（依總金額降冪，取前 N 筆）。 */
    @Query(nativeQuery = true, value = """
        WITH deduped AS (
            SELECT DISTINCT ON (tender_number, award_announce_date, award_announce_seq, vendor_order_seq)
                agency_name, agency_code, vendor_award_amount
            FROM tender_award
            WHERE tenant_id = :tenantId
              AND ((NULLIF(:taxId, '') IS NOT NULL AND vendor_tax_id = :taxId)
                OR (NULLIF(:taxId, '') IS NULL AND vendor_name = :vendorName))
            ORDER BY tender_number, award_announce_date, award_announce_seq, vendor_order_seq
        )
        SELECT agency_name,
               agency_code,
               COUNT(*) AS count,
               COALESCE(SUM(vendor_award_amount), 0) AS total_amount
        FROM deduped
        GROUP BY agency_name, agency_code
        ORDER BY total_amount DESC
        LIMIT :lim
    """)
    List<VendorProjections.Agency> getTopAgencies(
            @Param("tenantId") String tenantId,
            @Param("taxId") String taxId,
            @Param("vendorName") String vendorName,
            @Param("lim") int limit);

    /** 招標方式分布。 */
    @Query(nativeQuery = true, value = """
        WITH deduped AS (
            SELECT DISTINCT ON (tender_number, award_announce_date, award_announce_seq, vendor_order_seq)
                tender_method
            FROM tender_award
            WHERE tenant_id = :tenantId
              AND ((NULLIF(:taxId, '') IS NOT NULL AND vendor_tax_id = :taxId)
                OR (NULLIF(:taxId, '') IS NULL AND vendor_name = :vendorName))
            ORDER BY tender_number, award_announce_date, award_announce_seq, vendor_order_seq
        )
        SELECT COALESCE(NULLIF(tender_method, ''), '未填') AS type_name,
               COUNT(*) AS count
        FROM deduped
        GROUP BY tender_method
        ORDER BY count DESC
    """)
    List<VendorProjections.TypeCount> countByTenderMethod(
            @Param("tenantId") String tenantId,
            @Param("taxId") String taxId,
            @Param("vendorName") String vendorName);

    /** 採購類型分布。 */
    @Query(nativeQuery = true, value = """
        WITH deduped AS (
            SELECT DISTINCT ON (tender_number, award_announce_date, award_announce_seq, vendor_order_seq)
                procurement_type
            FROM tender_award
            WHERE tenant_id = :tenantId
              AND ((NULLIF(:taxId, '') IS NOT NULL AND vendor_tax_id = :taxId)
                OR (NULLIF(:taxId, '') IS NULL AND vendor_name = :vendorName))
            ORDER BY tender_number, award_announce_date, award_announce_seq, vendor_order_seq
        )
        SELECT COALESCE(NULLIF(procurement_type, ''), '未填') AS type_name,
               COUNT(*) AS count
        FROM deduped
        GROUP BY procurement_type
        ORDER BY count DESC
    """)
    List<VendorProjections.TypeCount> countByProcurementType(
            @Param("tenantId") String tenantId,
            @Param("taxId") String taxId,
            @Param("vendorName") String vendorName);

    /** 決標方式分布。 */
    @Query(nativeQuery = true, value = """
        WITH deduped AS (
            SELECT DISTINCT ON (tender_number, award_announce_date, award_announce_seq, vendor_order_seq)
                award_method
            FROM tender_award
            WHERE tenant_id = :tenantId
              AND ((NULLIF(:taxId, '') IS NOT NULL AND vendor_tax_id = :taxId)
                OR (NULLIF(:taxId, '') IS NULL AND vendor_name = :vendorName))
            ORDER BY tender_number, award_announce_date, award_announce_seq, vendor_order_seq
        )
        SELECT COALESCE(NULLIF(award_method, ''), '未填') AS type_name,
               COUNT(*) AS count
        FROM deduped
        GROUP BY award_method
        ORDER BY count DESC
    """)
    List<VendorProjections.TypeCount> countByAwardMethod(
            @Param("tenantId") String tenantId,
            @Param("taxId") String taxId,
            @Param("vendorName") String vendorName);

    /**
     * 共同得標廠商排行：與同一標案（tender_number + award_announce_seq）
     * 中其他廠商的共同次數。
     */
    @Query(nativeQuery = true, value = """
        SELECT b.vendor_name,
               b.vendor_tax_id,
               COUNT(DISTINCT a.tender_number || '|' || a.award_announce_seq) AS co_count
        FROM tender_award a
        JOIN tender_award b
          ON a.tender_number = b.tender_number
         AND a.award_announce_seq = b.award_announce_seq
         AND a.tenant_id = b.tenant_id
        WHERE a.tenant_id = :tenantId
          AND ((NULLIF(:taxId, '') IS NOT NULL AND a.vendor_tax_id = :taxId)
            OR (NULLIF(:taxId, '') IS NULL AND a.vendor_name = :vendorName))
          AND NOT ((NULLIF(:taxId, '') IS NOT NULL AND b.vendor_tax_id = :taxId)
               OR (NULLIF(:taxId, '') IS NULL AND b.vendor_name = :vendorName))
        GROUP BY b.vendor_name, b.vendor_tax_id
        ORDER BY co_count DESC
        LIMIT 10
    """)
    List<VendorProjections.CoVendor> getCoVendors(
            @Param("tenantId") String tenantId,
            @Param("taxId") String taxId,
            @Param("vendorName") String vendorName);

    // ── Solution 競品分析查詢 ─────────────────────────────────────────────────

    /** 取得所有 distinct solution 名稱（按字母排序，供下拉選單）。 */
    @Query(nativeQuery = true, value = """
        SELECT DISTINCT solution
        FROM tender_award
        WHERE tenant_id = :tenantId
          AND solution IS NOT NULL
        ORDER BY solution
    """)
    List<VendorProjections.SolutionOption> findDistinctSolutions(
            @Param("tenantId") String tenantId);

    /**
     * Solution 競品分析 KPI 摘要。
     * deduped CTE：先以 (tender_number, award_announce_date, award_announce_seq, vendor_order_seq) 去重，
     * 避免同一標案被多個 matched_keyword 爬到而重複計算件數與金額。
     * keyword_count 另外計算（不需去重，本身就是維度統計）。
     */
    @Query(nativeQuery = true, value = """
        WITH deduped AS (
            SELECT DISTINCT ON (tender_number, award_announce_date, award_announce_seq, vendor_order_seq)
                vendor_tax_id, vendor_name, vendor_award_amount
            FROM tender_award
            WHERE tenant_id = :tenantId
              AND solution = :solution
              AND (:keyword  IS NULL OR matched_keyword = :keyword)
              AND (:dateFrom IS NULL OR award_announce_date >= CAST(:dateFrom AS date))
              AND (:dateTo   IS NULL OR award_announce_date <= CAST(:dateTo   AS date))
            ORDER BY tender_number, award_announce_date, award_announce_seq, vendor_order_seq
        ),
        kw_cnt AS (
            SELECT COUNT(DISTINCT matched_keyword) AS keyword_count
            FROM tender_award
            WHERE tenant_id = :tenantId
              AND solution = :solution
              AND (:dateFrom IS NULL OR award_announce_date >= CAST(:dateFrom AS date))
              AND (:dateTo   IS NULL OR award_announce_date <= CAST(:dateTo   AS date))
        )
        SELECT
            (SELECT COUNT(*)                                           FROM deduped) AS total_tenders,
            (SELECT COALESCE(SUM(vendor_award_amount), 0)             FROM deduped) AS total_amount,
            (SELECT COUNT(DISTINCT COALESCE(vendor_tax_id, vendor_name)) FROM deduped) AS vendor_count,
            (SELECT keyword_count                                      FROM kw_cnt)  AS keyword_count
    """)
    List<VendorProjections.SolutionOverview> getSolutionOverview(
            @Param("tenantId") String tenantId,
            @Param("solution") String solution,
            @Param("keyword")  String keyword,
            @Param("dateFrom") String dateFrom,
            @Param("dateTo")   String dateTo);

    /**
     * Solution 競品分析廠商排行，依得標件數降冪，支援分頁。
     * COUNT query 為 Spring Data 必要的 countQuery 參數。
     */
    /**
     * Solution 競品分析廠商排行。
     * deduped CTE 先對實體標案去重，再彙總，避免同標案多關鍵字導致件數虛增。
     */
    @Query(
        nativeQuery = true,
        value = """
            WITH deduped AS (
                SELECT DISTINCT ON (tender_number, award_announce_date, award_announce_seq, vendor_order_seq)
                    vendor_tax_id, vendor_name, vendor_award_amount
                FROM tender_award
                WHERE tenant_id = :tenantId
                  AND solution = :solution
                  AND (:keyword  IS NULL OR matched_keyword = :keyword)
                  AND (:dateFrom IS NULL OR award_announce_date >= CAST(:dateFrom AS date))
                  AND (:dateTo   IS NULL OR award_announce_date <= CAST(:dateTo   AS date))
                ORDER BY tender_number, award_announce_date, award_announce_seq, vendor_order_seq
            )
            SELECT COALESCE(NULLIF(vendor_tax_id, ''), vendor_name) AS vendor_tax_id,
                   MAX(vendor_name)                                  AS vendor_name,
                   COUNT(*)                                          AS win_count,
                   COALESCE(SUM(vendor_award_amount), 0)            AS total_amount
            FROM deduped
            GROUP BY COALESCE(NULLIF(vendor_tax_id, ''), vendor_name)
            ORDER BY win_count DESC, total_amount DESC
        """,
        countQuery = """
            SELECT COUNT(*) FROM (
                WITH deduped AS (
                    SELECT DISTINCT ON (tender_number, award_announce_date, award_announce_seq, vendor_order_seq)
                        vendor_tax_id, vendor_name
                    FROM tender_award
                    WHERE tenant_id = :tenantId
                      AND solution = :solution
                      AND (:keyword  IS NULL OR matched_keyword = :keyword)
                      AND (:dateFrom IS NULL OR award_announce_date >= CAST(:dateFrom AS date))
                      AND (:dateTo   IS NULL OR award_announce_date <= CAST(:dateTo   AS date))
                    ORDER BY tender_number, award_announce_date, award_announce_seq, vendor_order_seq
                )
                SELECT COALESCE(NULLIF(vendor_tax_id, ''), vendor_name)
                FROM deduped
                GROUP BY COALESCE(NULLIF(vendor_tax_id, ''), vendor_name)
            ) sub
        """
    )
    Page<VendorProjections.SolutionVendorRank> getVendorRankBySolution(
            @Param("tenantId") String tenantId,
            @Param("solution") String solution,
            @Param("keyword")  String keyword,
            @Param("dateFrom") String dateFrom,
            @Param("dateTo")   String dateTo,
            Pageable pageable);

    /**
     * Solution 下各 matched_keyword 的統計（用於長條圖）。
     * 各 keyword 先對實體標案去重，再各自彙總件數/廠商數/金額，
     * 因此「監視系統」與「錄影」雖指向同一標案，各自的件數仍代表該 keyword 確實爬到的標案數。
     */
    @Query(nativeQuery = true, value = """
        SELECT matched_keyword AS keyword,
               COUNT(DISTINCT COALESCE(NULLIF(vendor_tax_id,''), vendor_name)) AS vendor_count,
               COUNT(DISTINCT tender_number || '|' || CAST(award_announce_date AS TEXT)
                              || '|' || award_announce_seq || '|' || CAST(vendor_order_seq AS TEXT)) AS win_count,
               COALESCE(SUM(vendor_award_amount), 0) AS total_amount
        FROM tender_award
        WHERE tenant_id = :tenantId
          AND solution = :solution
          AND (:dateFrom IS NULL OR award_announce_date >= CAST(:dateFrom AS date))
          AND (:dateTo   IS NULL OR award_announce_date <= CAST(:dateTo   AS date))
        GROUP BY matched_keyword
        ORDER BY win_count DESC
    """)
    List<VendorProjections.SolutionKeyword> getKeywordSummaryBySolution(
            @Param("tenantId") String tenantId,
            @Param("solution") String solution,
            @Param("dateFrom") String dateFrom,
            @Param("dateTo")   String dateTo);
}
