package com.taipei.iot.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * 圖片消毒 — 透過 ImageIO re-encode 消除 EXIF metadata 及 Polyglot payload。
 * <p>
 * 原理：{@code ImageIO.read()} 解析圖片像素後，
 * {@code ImageIO.write()} 只輸出純像素資料，
 * 任何嵌入的 EXIF、XMP、Polyglot 內容都會被丟棄。
 * </p>
 */
@Slf4j
@Component
public class ImageSanitizer {

    /**
     * 將圖片重新編碼，消除潛在的惡意 metadata。
     *
     * @param input     原始圖片串流
     * @param extension 副檔名（jpg, png, gif 等）
     * @return 消毒後的圖片位元組；若無法處理則回傳 null
     */
    public byte[] sanitize(InputStream input, String extension) {
        try {
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                log.warn("ImageIO 無法解析圖片 (extension={})", extension);
                return null;
            }

            String formatName = mapToFormatName(extension);
            if (formatName == null) {
                log.debug("不支援的圖片格式: {}", extension);
                return null;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean written = ImageIO.write(image, formatName, baos);
            if (!written) {
                log.warn("ImageIO.write failed for format: {}", formatName);
                return null;
            }

            log.debug("Image sanitized: extension={}, outputSize={}", extension, baos.size());
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("圖片消毒失敗: {}", e.getMessage());
            return null;
        }
    }

    private String mapToFormatName(String extension) {
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "JPEG";
            case "png" -> "PNG";
            case "gif" -> "GIF";
            case "bmp" -> "BMP";
            case "webp" -> "WEBP";
            default -> null;
        };
    }
}
