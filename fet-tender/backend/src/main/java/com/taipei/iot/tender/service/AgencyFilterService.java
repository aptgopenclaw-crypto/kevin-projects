package com.taipei.iot.tender.service;

import com.taipei.iot.tender.dto.AgencyFilterRequest;
import com.taipei.iot.tender.dto.AgencyFilterResponse;
import com.taipei.iot.tender.entity.AnnouncementAgencyFilter;
import com.taipei.iot.tender.repository.AnnouncementAgencyFilterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgencyFilterService {

    private final AnnouncementAgencyFilterRepository repository;

    @Transactional(readOnly = true)
    public List<AgencyFilterResponse> listAll() {
        return repository.findByIsActiveTrueOrderBySolutionAscAgencyKeywordAsc()
                .stream().map(AgencyFilterResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AgencyFilterResponse> listAllIncludeInactive() {
        return repository.findAll().stream().map(AgencyFilterResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AgencyFilterResponse> listBySolution(String solution) {
        return repository.findBySolutionAndIsActiveTrue(solution)
                .stream().map(AgencyFilterResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AgencyFilterResponse> listBySolutionIncludeInactive(String solution) {
        return repository.findBySolutionOrderByAgencyKeywordAsc(solution)
                .stream().map(AgencyFilterResponse::from).toList();
    }

    @Transactional
    public AgencyFilterResponse create(AgencyFilterRequest req) {
        AnnouncementAgencyFilter entity = req.toEntity();
        return AgencyFilterResponse.from(repository.save(entity));
    }

    @Transactional
    public AgencyFilterResponse update(Long id, AgencyFilterRequest req) {
        AnnouncementAgencyFilter entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("機關過濾設定不存在: " + id));
        entity.setSolution(req.getSolution().trim());
        entity.setAgencyKeyword(req.getAgencyKeyword().trim());
        if (req.getIsOrgOnlySearch() != null) {
            entity.setIsOrgOnlySearch(req.getIsOrgOnlySearch());
        }
        if (req.getIsActive() != null) {
            entity.setIsActive(req.getIsActive());
        }
        return AgencyFilterResponse.from(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("機關過濾設定不存在: " + id);
        }
        repository.deleteById(id);
    }
}
