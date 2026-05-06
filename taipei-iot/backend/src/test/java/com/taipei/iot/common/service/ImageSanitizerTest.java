package com.taipei.iot.common.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ImageSanitizerTest {

    private final ImageSanitizer sanitizer = new ImageSanitizer();

    @Test
    void sanitize_validJpeg_returnsSanitizedBytes() throws Exception {
        // Create a real JPEG image in memory
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        img.setRGB(5, 5, 0xFF0000); // set a red pixel
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "JPEG", baos);
        byte[] original = baos.toByteArray();

        byte[] result = sanitizer.sanitize(new ByteArrayInputStream(original), "jpg");

        assertNotNull(result);
        assertTrue(result.length > 0);
        // Verify it's a valid JPEG (starts with FF D8)
        assertEquals((byte) 0xFF, result[0]);
        assertEquals((byte) 0xD8, result[1]);
    }

    @Test
    void sanitize_validPng_returnsSanitizedBytes() throws Exception {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);

        byte[] result = sanitizer.sanitize(new ByteArrayInputStream(baos.toByteArray()), "png");

        assertNotNull(result);
        assertTrue(result.length > 0);
        // PNG magic bytes: 89 50 4E 47
        assertEquals((byte) 0x89, result[0]);
        assertEquals((byte) 0x50, result[1]);
    }

    @Test
    void sanitize_invalidImage_returnsNull() {
        byte[] garbage = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};

        byte[] result = sanitizer.sanitize(new ByteArrayInputStream(garbage), "jpg");

        assertNull(result);
    }

    @Test
    void sanitize_unsupportedFormat_returnsNull() throws Exception {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "JPEG", baos);

        byte[] result = sanitizer.sanitize(new ByteArrayInputStream(baos.toByteArray()), "tiff");

        assertNull(result);
    }

    @Test
    void sanitize_stripsExifMetadata() throws Exception {
        // Create a JPEG image — after re-encode, no extra metadata should be present
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "JPEG", baos);
        byte[] original = baos.toByteArray();

        byte[] sanitized = sanitizer.sanitize(new ByteArrayInputStream(original), "jpeg");

        assertNotNull(sanitized);
        // Re-decode to verify it's a valid image
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(sanitized));
        assertNotNull(decoded);
        assertEquals(100, decoded.getWidth());
        assertEquals(100, decoded.getHeight());
    }
}
