package com.taipei.iot.tender.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.tender.dto.SearchKeywordRequest;
import com.taipei.iot.tender.dto.SearchKeywordResponse;
import com.taipei.iot.tender.service.SearchKeywordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/tender/announcement-keywords")
@RequiredArgsConstructor
public class SearchKeywordController {

    private final SearchKeywordService service;

    @GetMapping
    @PreAuthorize("hasAuthority('tender:config:view')")
    public BaseResponse<List<SearchKeywordResponse>> list(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        List<SearchKeywordResponse> result = includeInactive
                ? service.listAllIncludeInactive()
                : service.listAll();
        return BaseResponse.success(result);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('tender:config:edit')")
    public BaseResponse<SearchKeywordResponse> create(@Valid @RequestBody SearchKeywordRequest req) {
        return BaseResponse.success(service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('tender:config:edit')")
    public BaseResponse<SearchKeywordResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SearchKeywordRequest req) {
        return BaseResponse.success(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('tender:config:edit')")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return BaseResponse.success(null);
    }

    /**
     * 取得所有方案名稱選項（合併搜尋關鍵字 + 機關過濾的 distinct solutions）。
     */
    @GetMapping("/solutions")
    @PreAuthorize("hasAuthority('tender:config:view')")
    public BaseResponse<List<String>> listSolutions() {
        return BaseResponse.success(service.listDistinctSolutions());
    }
}
