package com.taipei.iot.tender.service;

import com.taipei.iot.tender.dto.TenderAnnouncementQueryRequest;
import com.taipei.iot.tender.dto.TenderAnnouncementResponse;
import com.taipei.iot.tender.entity.TenderAnnouncement;
import com.taipei.iot.tender.repository.TenderAnnouncementRepository;
import com.taipei.iot.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenderAnnouncementService {

    private final TenderAnnouncementRepository repository;

    @Transactional(readOnly = true)
    public PageResponse<TenderAnnouncementResponse> search(TenderAnnouncementQueryRequest req) {
        var pageable = PageRequest.of(req.getPage(), req.getSize());
        Page<TenderAnnouncementResponse> page = repository.search(
                req.getSolution(),
                req.getKeyword(),
                req.getAgency(),
                req.getName(),
                req.getDateFrom(),
                req.getDateTo(),
                pageable
        ).map(TenderAnnouncementResponse::from);

        return PageResponse.<TenderAnnouncementResponse>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .size(page.getSize())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TenderAnnouncement> queryForExport(TenderAnnouncementQueryRequest req) {
        return repository.searchForExport(
                req.getSolution(),
                req.getKeyword(),
                req.getAgency(),
                req.getName(),
                req.getDateFrom(),
                req.getDateTo()
        );
    }

    @Transactional(readOnly = true)
    public TenderAnnouncementResponse getById(Long id) {
        return repository.findById(id)
                .map(TenderAnnouncementResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("招標公告不存在: " + id));
    }

    @Transactional
    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("招標公告不存在: " + id);
        }
        repository.deleteById(id);
    }
}
