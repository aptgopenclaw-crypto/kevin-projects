package com.taipei.iot.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * NoOp 病毒掃描 — 開發環境使用，所有檔案直接回傳 CLEAN。
 * <p>當 {@code virus-scan.enabled=false}（預設）時啟用。</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "virus-scan.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpVirusScanService implements VirusScanService {

    @Override
    public ScanResult scan(String filePath) {
        log.debug("NoOp virus scan — skipping: {}", filePath);
        return ScanResult.CLEAN;
    }
}
