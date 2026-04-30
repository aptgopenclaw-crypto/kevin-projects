package com.taipei.iot.common.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 檔案儲存抽象介面。
 * 目前實作為 LocalFileStorageService（本地磁碟），
 * 後續可替換為 S3/MinIO 實作。
 */
public interface FileStorageService {

    /**
     * 儲存檔案，回傳相對路徑（可作為 file_url 存入 DB）。
     *
     * @param subDir   子目錄（如 "repair/123"）
     * @param fileName 檔案名稱
     * @param input    檔案內容
     * @return 儲存後的相對路徑
     */
    String store(String subDir, String fileName, InputStream input);

    /**
     * 便利方法：直接傳入 MultipartFile。
     */
    String store(String subDir, MultipartFile file);

    /**
     * 讀取檔案。
     */
    InputStream load(String path);

    /**
     * 取得檔案的絕對路徑（供病毒掃描等外部工具使用）。
     */
    String resolveAbsolutePath(String path);

    /**
     * 刪除檔案。
     */
    void delete(String path);
}
