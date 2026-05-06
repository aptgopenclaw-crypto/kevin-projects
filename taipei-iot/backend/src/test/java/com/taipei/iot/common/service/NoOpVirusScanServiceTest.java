package com.taipei.iot.common.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoOpVirusScanServiceTest {

    private final NoOpVirusScanService service = new NoOpVirusScanService();

    @Test
    void scan_alwaysReturnsClean() {
        assertEquals(VirusScanService.ScanResult.CLEAN, service.scan("/any/path/file.jpg"));
    }

    @Test
    void scan_nullPath_returnsClean() {
        assertEquals(VirusScanService.ScanResult.CLEAN, service.scan(null));
    }
}
