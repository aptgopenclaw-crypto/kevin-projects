package com.taipei.iot.tender.service;

import com.taipei.iot.tender.dto.TenderAwardQueryRequest;
import com.taipei.iot.tender.dto.TenderAwardResponse;
import com.taipei.iot.tender.repository.TenderAwardRepository;
import com.taipei.iot.user.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenderAwardService {

    private final TenderAwardRepository repository;

    @Transactional(readOnly = true)
    public PageResponse<TenderAwardResponse> search(TenderAwardQueryRequest req) {
        var pageable = PageRequest.of(req.getPage(), req.getSize());
        // LocalDate → String 避免 PostgreSQL 對 null 無法推斷型別（could not determine data type of parameter）
        String dateFrom = req.getDateFrom() != null ? req.getDateFrom().toString() : null;
        String dateTo   = req.getDateTo()   != null ? req.getDateTo().toString()   : null;
        Page<TenderAwardResponse> page = repository.search(
                req.getSolution(),
                req.getKeyword(),
                req.getAgency(),
                req.getName(),
                req.getVendorName(),
                dateFrom,
                dateTo,
                pageable
        ).map(TenderAwardResponse::from);

        return PageResponse.<TenderAwardResponse>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .size(page.getSize())
                .build();
    }

    @Transactional(readOnly = true)
    public TenderAwardResponse getById(Long id) {
        return repository.findById(id)
                .map(TenderAwardResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("決標公告不存在: " + id));
    }

    @Transactional
    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("決標公告不存在: " + id);
        }
        repository.deleteById(id);
    }
}
