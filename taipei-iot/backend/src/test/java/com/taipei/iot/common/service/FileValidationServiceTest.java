package com.taipei.iot.common.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileValidationServiceTest {

    // 圖片 5MB, 文件 20MB, 影音 100MB
    private final FileValidationService service = new FileValidationService(
            5 * 1024 * 1024, 20 * 1024 * 1024, 100 * 1024 * 1024);

    // ── 副檔名白名單 ──

    @Test
    void validateExtension_jpg_allowed() {
        assertEquals("jpg", service.validateExtension("photo.jpg"));
    }

    @Test
    void validateExtension_jpeg_allowed() {
        assertEquals("jpeg", service.validateExtension("photo.jpeg"));
    }

    @Test
    void validateExtension_pdf_allowed() {
        assertEquals("pdf", service.validateExtension("report.pdf"));
    }

    @Test
    void validateExtension_xlsx_allowed() {
        assertEquals("xlsx", service.validateExtension("data.xlsx"));
    }

    @Test
    void validateExtension_exe_rejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validateExtension("malware.exe"));
        assertEquals(ErrorCode.FILE_EXTENSION_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void validateExtension_php_rejected() {
        assertThrows(BusinessException.class, () -> service.validateExtension("shell.php"));
    }

    @Test
    void validateExtension_jsp_rejected() {
        assertThrows(BusinessException.class, () -> service.validateExtension("webshell.jsp"));
    }

    @Test
    void validateExtension_noExtension_rejected() {
        assertThrows(BusinessException.class, () -> service.validateExtension("noext"));
    }

    @Test
    void validateExtension_null_rejected() {
        assertThrows(BusinessException.class, () -> service.validateExtension(null));
    }

    @Test
    void validateExtension_caseInsensitive() {
        assertEquals("png", service.validateExtension("image.PNG"));
    }

    // ── 檔案大小（按類別） ──

    @Test
    void validateSize_imageWithinLimit_ok() {
        assertDoesNotThrow(() -> service.validateSize(4 * 1024 * 1024, "jpg")); // 4MB < 5MB
    }

    @Test
    void validateSize_imageExceedsLimit_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validateSize(6 * 1024 * 1024, "png")); // 6MB > 5MB
        assertEquals(ErrorCode.FILE_SIZE_EXCEEDED, ex.getErrorCode());
    }

    @Test
    void validateSize_imageExactlyAtLimit_ok() {
        assertDoesNotThrow(() -> service.validateSize(5 * 1024 * 1024, "webp")); // 5MB == 5MB
    }

    @Test
    void validateSize_documentWithinLimit_ok() {
        assertDoesNotThrow(() -> service.validateSize(15 * 1024 * 1024, "pdf")); // 15MB < 20MB
    }

    @Test
    void validateSize_documentExceedsLimit_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validateSize(21 * 1024 * 1024, "xlsx")); // 21MB > 20MB
        assertEquals(ErrorCode.FILE_SIZE_EXCEEDED, ex.getErrorCode());
    }

    @Test
    void validateSize_mediaWithinLimit_ok() {
        assertDoesNotThrow(() -> service.validateSize(80 * 1024 * 1024, "mp4")); // 80MB < 100MB
    }

    @Test
    void validateSize_mediaExceedsLimit_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validateSize(101L * 1024 * 1024, "mp4")); // 101MB > 100MB
        assertEquals(ErrorCode.FILE_SIZE_EXCEEDED, ex.getErrorCode());
    }

    @Test
    void validateSize_nullExtension_usesDocumentLimit() {
        assertDoesNotThrow(() -> service.validateSize(19 * 1024 * 1024, null)); // 19MB < 20MB
        assertThrows(BusinessException.class,
                () -> service.validateSize(21 * 1024 * 1024, null)); // 21MB > 20MB
    }

    // ── Magic bytes ──

    @Test
    void validateMagicBytes_jpegFile_matches() throws IOException {
        // Real JPEG magic bytes: FF D8 FF
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0};
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(jpegBytes));
        when(file.getOriginalFilename()).thenReturn("photo.jpg");

        // Should not throw — JPEG magic bytes match .jpg extension
        assertDoesNotThrow(() -> service.validateMagicBytes(file, "jpg"));
    }

    @Test
    void validateMagicBytes_pngMaskedAsJpg_throws() throws IOException {
        // Real PNG magic bytes: 89 50 4E 47 0D 0A 1A 0A
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(pngBytes));
        when(file.getOriginalFilename()).thenReturn("fake.jpg");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validateMagicBytes(file, "jpg"));
        assertEquals(ErrorCode.FILE_TYPE_MISMATCH, ex.getErrorCode());
    }

    // ── isImage ──

    @Test
    void isImage_jpg_true() {
        assertTrue(service.isImage("jpg"));
    }

    @Test
    void isImage_pdf_false() {
        assertFalse(service.isImage("pdf"));
    }

    @Test
    void isImage_null_false() {
        assertFalse(service.isImage(null));
    }

    // ── validate (整合) ──

    @Test
    void validate_emptyFile_throws() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.validate(file));
    }
}
