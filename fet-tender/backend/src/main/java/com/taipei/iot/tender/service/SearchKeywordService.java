package com.taipei.iot.tender.service;

import com.taipei.iot.tender.dto.SearchKeywordRequest;
import com.taipei.iot.tender.dto.SearchKeywordResponse;
import com.taipei.iot.tender.entity.AnnouncementSearchKeyword;
import com.taipei.iot.tender.repository.AnnouncementSearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchKeywordService {

    private final AnnouncementSearchKeywordRepository repository;

    @Transactional(readOnly = true)
    public List<SearchKeywordResponse> listAll() {
        return repository.findByIsActiveTrueOrderBySolutionAscKeywordAsc()
                .stream().map(SearchKeywordResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SearchKeywordResponse> listAllIncludeInactive() {
        return repository.findAll().stream().map(SearchKeywordResponse::from).toList();
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
}
