package com.taipei.iot.announcement.repository;

import com.taipei.iot.announcement.entity.Announcement;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long>, TenantScopedRepository {

    /**
     * 前台查詢：已發佈 + 未過期 + 受眾範圍（ALL 或使用者部門在 announcement_depts 中）
     */
    @Query("""
        SELECT a FROM Announcement a
        WHERE a.status = 'PUBLISHED'
          AND a.publishAt <= :now
          AND (a.expireAt IS NULL OR a.expireAt > :now)
          AND (a.scope = 'ALL'
               OR EXISTS (SELECT 1 FROM AnnouncementDept ad
                          WHERE ad.announcementId = a.id AND ad.deptId = :userDeptId))
        """)
    Page<Announcement> findVisibleAnnouncements(
            @Param("userDeptId") Long userDeptId,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    /**
     * 管理頁面：ADMIN 看全部
     */
    @Query("""
        SELECT a FROM Announcement a
        WHERE (:statusFilter = 'ALL' OR
               (:statusFilter = 'DRAFT' AND a.status = 'DRAFT') OR
               (:statusFilter = 'PUBLISHED' AND a.status = 'PUBLISHED'
                   AND (a.expireAt IS NULL OR a.expireAt > :now)) OR
               (:statusFilter = 'EXPIRED' AND a.status = 'PUBLISHED'
                   AND a.expireAt IS NOT NULL AND a.expireAt < :now))
          AND (:keyword IS NULL OR a.title LIKE :keyword)
        """)
    Page<Announcement> findAdminAnnouncements(
            @Param("statusFilter") String statusFilter,
            @Param("keyword") String keyword,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    /**
     * 管理頁面：DEPT_ADMIN 看自己建立的 + 受眾包含自己部門的
     */
    @Query("""
        SELECT a FROM Announcement a
        WHERE (a.createdBy = :userId
               OR (a.scope = 'DEPT' AND EXISTS (
                   SELECT 1 FROM AnnouncementDept ad
                   WHERE ad.announcementId = a.id AND ad.deptId = :userDeptId)))
          AND (:statusFilter = 'ALL' OR
               (:statusFilter = 'DRAFT' AND a.status = 'DRAFT') OR
               (:statusFilter = 'PUBLISHED' AND a.status = 'PUBLISHED'
                   AND (a.expireAt IS NULL OR a.expireAt > :now)) OR
               (:statusFilter = 'EXPIRED' AND a.status = 'PUBLISHED'
                   AND a.expireAt IS NOT NULL AND a.expireAt < :now))
          AND (:keyword IS NULL OR a.title LIKE :keyword)
        """)
    Page<Announcement> findDeptAdminAnnouncements(
            @Param("userId") String userId,
            @Param("userDeptId") Long userDeptId,
            @Param("statusFilter") String statusFilter,
            @Param("keyword") String keyword,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    /**
     * 未讀數量：已發佈 + 未過期 + 受眾範圍 + 未讀
     */
    @Query("""
        SELECT COUNT(a) FROM Announcement a
        WHERE a.status = 'PUBLISHED'
          AND a.publishAt <= :now
          AND (a.expireAt IS NULL OR a.expireAt > :now)
          AND (a.scope = 'ALL'
               OR EXISTS (SELECT 1 FROM AnnouncementDept ad
                          WHERE ad.announcementId = a.id AND ad.deptId = :userDeptId))
          AND NOT EXISTS (SELECT 1 FROM AnnouncementRead r
                          WHERE r.announcementId = a.id AND r.userId = :userId)
        """)
    long countUnread(
            @Param("userDeptId") Long userDeptId,
            @Param("userId") String userId,
            @Param("now") LocalDateTime now);
}
