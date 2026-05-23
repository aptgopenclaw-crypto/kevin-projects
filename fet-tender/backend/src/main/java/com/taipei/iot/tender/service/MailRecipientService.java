package com.taipei.iot.tender.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.tender.dto.MailRecipientBatchResult;
import com.taipei.iot.tender.dto.MailRecipientRequest;
import com.taipei.iot.tender.dto.MailRecipientResponse;
import com.taipei.iot.tender.entity.TenderMailRecipient;
import com.taipei.iot.tender.repository.TenderMailRecipientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MailRecipientService {

    private final TenderMailRecipientRepository repository;

    @Transactional(readOnly = true)
    public List<MailRecipientResponse> listAll() {
        return repository.findByIsActiveTrueOrderByNameAsc()
                .stream().map(MailRecipientResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MailRecipientResponse> listAllIncludeInactive() {
        return repository.findAllByOrderByNameAsc()
                .stream().map(MailRecipientResponse::from).toList();
    }

    /**
     * 取得啟用中的收件人 email 清單（供 TenderMailService 使用）。
     */
    @Transactional(readOnly = true)
    public List<String> getActiveEmails() {
        return repository.findByIsActiveTrueOrderByNameAsc()
                .stream().map(TenderMailRecipient::getEmail).toList();
    }

    @Transactional
    public MailRecipientResponse create(MailRecipientRequest req) {
        TenderMailRecipient entity = req.toEntity();
        try {
            return MailRecipientResponse.from(repository.save(entity));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.MAIL_RECIPIENT_EMAIL_DUPLICATE);
        }
    }

    @Transactional
    public MailRecipientResponse update(Long id, MailRecipientRequest req) {
        TenderMailRecipient entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("收件人設定不存在: " + id));
        entity.setEmail(req.getEmail().trim().toLowerCase());
        entity.setName(req.getName() != null ? req.getName().trim() : null);
        if (req.getIsActive() != null) {
            entity.setIsActive(req.getIsActive());
        }
        return MailRecipientResponse.from(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("收件人設定不存在: " + id);
        }
        repository.deleteById(id);
    }

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @Transactional
    public MailRecipientBatchResult batchImport(List<String> emails) {
        List<MailRecipientBatchResult.SkippedItem> skipped = new ArrayList<>();
        int successCount = 0;

        for (String raw : emails) {
            String email = raw == null ? "" : raw.trim().toLowerCase();
            if (email.isEmpty()) continue;

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                skipped.add(MailRecipientBatchResult.SkippedItem.builder()
                        .email(raw.trim()).reason("格式錯誤").build());
                continue;
            }

            TenderMailRecipient entity = TenderMailRecipient.builder()
                    .email(email)
                    .isActive(true)
                    .build();
            try {
                repository.save(entity);
                successCount++;
            } catch (DataIntegrityViolationException e) {
                skipped.add(MailRecipientBatchResult.SkippedItem.builder()
                        .email(email).reason("已存在").build());
            }
        }

        return MailRecipientBatchResult.builder()
                .successCount(successCount)
                .skippedCount(skipped.size())
                .skippedItems(skipped)
                .build();
    }
}
