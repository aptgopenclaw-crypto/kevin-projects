package com.taipei.iot.replacement.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QrCodeServiceTest {

    private final QrCodeService service = new QrCodeService();

    @Test
    void generatePng_returnsValidPngBytes() {
        byte[] result = service.generatePng("https://streetlight.taipei/public/repair?pole=PN-001");

        assertNotNull(result);
        assertTrue(result.length > 100, "PNG should have reasonable size");
        // PNG magic bytes: 0x89 0x50 0x4E 0x47
        assertEquals((byte) 0x89, result[0]);
        assertEquals((byte) 0x50, result[1]);
        assertEquals((byte) 0x4E, result[2]);
        assertEquals((byte) 0x47, result[3]);
    }

    @Test
    void generatePng_customSize() {
        byte[] small = service.generatePng("test", 100);
        byte[] large = service.generatePng("test", 500);

        assertNotNull(small);
        assertNotNull(large);
        assertTrue(large.length > small.length, "Larger size should produce larger PNG");
    }

    @Test
    void generatePng_chineseContent() {
        byte[] result = service.generatePng("臺北市路燈 PN-001");

        assertNotNull(result);
        assertTrue(result.length > 0);
    }
}
