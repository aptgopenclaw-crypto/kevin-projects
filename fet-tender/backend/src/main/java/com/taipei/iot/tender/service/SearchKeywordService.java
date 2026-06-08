package com.taipei.iot.tender.service;

import com.taipei.iot.tender.dto.SearchKeywordRequest;
import com.taipei.iot.tender.dto.SearchKeywordResponse;
import com.taipei.iot.tender.entity.AnnouncementSearchKeyword;
import com.taipei.iot.tender.repository.AnnouncementAgencyFilterRepository;
import com.taipei.iot.tender.repository.AnnouncementSearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SearchKeywordService {

    private final AnnouncementSearchKeywordRepository repository;
    private final AnnouncementAgencyFilterRepository agencyFilterRepository;

    @Transactional(readOnly = true)
    public List<SearchKeywordResponse> listAll() {
        return repository.findByIsActiveTrueOrderBySolutionAscKeywordAsc()
                .stream().map(SearchKeywordResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SearchKeywordResponse> listAllIncludeInactive() {
        return repository.findAll().stream().map(SearchKeywordResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SearchKeywordResponse> listBySolution(String solution) {
        return repository.findBySolutionAndIsActiveTrueOrderByKeywordAsc(solution)
                .stream().map(SearchKeywordResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SearchKeywordResponse> listBySolutionIncludeInactive(String solution) {
        return repository.findBySolutionOrderByKeywordAsc(solution)
                .stream().map(SearchKeywordResponse::from).toList();
    }

    @Transactional
    public SearchKeywordResponse create(SearchKeywordRequest req) {
        AnnouncementSearchKeyword entity = req.toEntity();
        return SearchKeywordResponse.from(repository.save(entity));
    }

    @Transactional
    public SearchKeywordResponse update(Long id, SearchKeywordRequest req) {
        AnnouncementSearchKeyword entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("關鍵字設定不存在: " + id));
        entity.setSolution(req.getSolution().trim());
        entity.setKeyword(req.getKeyword().trim());
        if (req.getIsActive() != null) {
            entity.setIsActive(req.getIsActive());
        }
        return SearchKeywordResponse.from(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("關鍵字設定不存在: " + id);
        }
        repository.deleteById(id);
    }

    /**
     * 取得所有已存在的方案名稱（合併 keywords + agency_filters 的 distinct solutions）。
     */
    @Transactional(readOnly = true)
    public List<String> listDistinctSolutions() {
        TreeSet<String> solutions = Stream.concat(
                repository.findDistinctSolutions().stream(),
                agencyFilterRepository.findDistinctSolutions().stream()
        ).collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        return List.copyOf(solutions);
    }
}
