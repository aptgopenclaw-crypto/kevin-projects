package com.taipei.iot.tender.repository;

import com.taipei.iot.tender.entity.TenderAward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 決標資料 Repository（fix-data 精簡版，僅包含爬蟲 upsert 所需方法）。
 */
public interface TenderAwardRepository extends JpaRepository<TenderAward, Long> {

    Optional<TenderAward> findBySolutionAndMatchedKeywordAndTenderNumberAndAwardAnnounceDateAndAwardAnnounceSeqAndVendorOrderSeq(
            String solution,
            String matchedKeyword,
            String tenderNumber,
            LocalDate awardAnnounceDate,
            String awardAnnounceSeq,
            Integer vendorOrderSeq);
}
