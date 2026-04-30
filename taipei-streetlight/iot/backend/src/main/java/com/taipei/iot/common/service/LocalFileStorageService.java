package com.taipei.iot.common.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 本地磁碟檔案儲存實作。
 * <p>後續可替換為 S3/MinIO 實作，只需實作 {@link FileStorageService} 介面。</p>
 */
@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path rootLocation;

    public LocalFileStorageService(@Value("${file.storage.local.root-dir:./uploads}") String rootDir) {
        this.rootLocation = Paths.get(rootDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("無法建立檔案儲存目錄: " + rootDir, e);
        }
    }

    @Override
    public String store(String subDir, String fileName, InputStream input) {
        try {
            String safeFileName = sanitizeFileName(fileName);
            String uniqueName = UUID.randomUUID().toString().substring(0, 8) + "_" + safeFileName;

            Path targetDir = rootLocation.resolve(subDir).normalize();
            if (!targetDir.startsWith(rootLocation)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "非法的儲存路徑");
            }
            Files.createDirectories(targetDir);

            Path targetFile = targetDir.resolve(uniqueName).normalize();
            if (!targetFile.startsWith(targetDir)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "非法的檔案名稱");
            }

            Files.copy(input, targetFile, StandardCopyOption.REPLACE_EXISTING);

            // 回傳相對路徑
            return rootLocation.relativize(targetFile).toString();
        } catch (IOException e) {
            log.error("檔案儲存失敗: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.ATTACHMENT_UPLOAD_FAILED);
        }
    }

    @Override
    public String store(String subDir, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "上傳檔案不可為空");
        }

        try {
            return store(subDir, file.getOriginalFilename(), file.getInputStream());
        } catch (IOException e) {
            log.error("讀取上傳檔案失敗: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.ATTACHMENT_UPLOAD_FAILED);
        }
    }

    @Override
    public InputStream load(String path) {
        try {
            Path file = rootLocation.resolve(path).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "非法的檔案路徑");
            }
            if (!Files.exists(file)) {
                throw new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND);
            }
            return Files.newInputStream(file);
        } catch (IOException e) {
            log.error("讀取檔案失敗: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND);
        }
    }

    @Override
    public void delete(String path) {
        try {
            Path file = rootLocation.resolve(path).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "非法的檔案路徑");
            }
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("刪除檔案失敗: {}", e.getMessage());
        }
    }

    @Override
    public String resolveAbsolutePath(String path) {
        Path file = rootLocation.resolve(path).normalize();
        if (!file.startsWith(rootLocation)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "非法的檔案路徑");
        }
        return file.toAbsolutePath().toString();
    }

    /**
     * 清除檔案名稱中的路徑穿越字元。
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "unnamed";
        }
        // 移除路徑分隔符號與 null bytes
        return fileName.replaceAll("[/\\\\\\x00]", "_")
                       .replaceAll("\\.\\.", "_");
    }
}
