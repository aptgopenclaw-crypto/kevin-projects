package com.taipei.iot.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ClamAV 病毒掃描 — 透過 clamd INSTREAM 協定掃描檔案。
 * <p>當 {@code virus-scan.enabled=true} 時啟用。</p>
 *
 * <h3>clamd INSTREAM 協定</h3>
 * <ol>
 *   <li>發送 {@code zINSTREAM\0}</li>
 *   <li>發送 chunk: 4 bytes (big-endian length) + data</li>
 *   <li>發送 zero-length chunk (4 bytes of 0) 結束</li>
 *   <li>讀取回應: {@code stream: OK\0} 或 {@code stream: <virus> FOUND\0}</li>
 * </ol>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "virus-scan.enabled", havingValue = "true")
public class ClamAvVirusScanService implements VirusScanService {

    private final String host;
    private final int port;
    private final int timeout;

    private static final int CHUNK_SIZE = 8192;

    public ClamAvVirusScanService(
            @Value("${virus-scan.clamav.host:localhost}") String host,
            @Value("${virus-scan.clamav.port:3310}") int port,
            @Value("${virus-scan.clamav.timeout:5000}") int timeout) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }

    @Override
    public ScanResult scan(String filePath) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            socket.setSoTimeout(timeout);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // 1. Send INSTREAM command
            out.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
            out.flush();

            // 2. Send file in chunks
            try (InputStream fileStream = Files.newInputStream(Path.of(filePath))) {
                byte[] buffer = new byte[CHUNK_SIZE];
                int bytesRead;
                while ((bytesRead = fileStream.read(buffer)) != -1) {
                    out.write(ByteBuffer.allocate(4).putInt(bytesRead).array());
                    out.write(buffer, 0, bytesRead);
                }
            }

            // 3. Send zero-length chunk to signal end
            out.write(new byte[]{0, 0, 0, 0});
            out.flush();

            // 4. Read response
            byte[] response = in.readAllBytes();
            String result = new String(response, StandardCharsets.US_ASCII).trim();

            log.info("ClamAV scan result for {}: {}", filePath, result);

            if (result.endsWith("OK")) {
                return ScanResult.CLEAN;
            } else if (result.contains("FOUND")) {
                log.warn("Virus detected in file {}: {}", filePath, result);
                return ScanResult.INFECTED;
            } else {
                log.error("Unexpected ClamAV response: {}", result);
                return ScanResult.ERROR;
            }
        } catch (IOException e) {
            log.error("ClamAV connection failed ({}:{}): {}", host, port, e.getMessage());
            return ScanResult.ERROR;
        }
    }
}
