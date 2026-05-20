package com.taipei.iot.user.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.user.entity.PasswordHistoryEntity;
import com.taipei.iot.user.repository.PasswordHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PasswordValidator {

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${user.password.min-length:8}")
    private int minLength;

    @Value("${user.password.require-uppercase:true}")
    private boolean requireUppercase;

    @Value("${user.password.require-lowercase:true}")
    private boolean requireLowercase;

    @Value("${user.password.require-digit:true}")
    private boolean requireDigit;

    @Value("${user.password.history-count:5}")
    private int historyCount;

    public void validate(String newPassword) {
        if (newPassword == null || newPassword.length() < minLength) {
            throw new BusinessException(ErrorCode.RESET_PASSWORD_ERROR,
                    "密碼長度至少 " + minLength + " 字元");
        }
        if (requireUppercase && !newPassword.chars().anyMatch(Character::isUpperCase)) {
            throw new BusinessException(ErrorCode.RESET_PASSWORD_ERROR,
                    "密碼必須包含大寫英文字母");
        }
        if (requireLowercase && !newPassword.chars().anyMatch(Character::isLowerCase)) {
            throw new BusinessException(ErrorCode.RESET_PASSWORD_ERROR,
                    "密碼必須包含小寫英文字母");
        }
        if (requireDigit && !newPassword.chars().anyMatch(Character::isDigit)) {
            throw new BusinessException(ErrorCode.RESET_PASSWORD_ERROR,
                    "密碼必須包含數字");
        }
    }

    public void checkNotRecentlyUsed(String userId, String rawPassword) {
        List<PasswordHistoryEntity> recentPasswords =
                passwordHistoryRepository.findTop5ByUserIdOrderByCreateTimeDesc(userId);
        for (PasswordHistoryEntity history : recentPasswords) {
            if (passwordEncoder.matches(rawPassword, history.getPasswordHash())) {
                throw new BusinessException(ErrorCode.PASSWORD_RECENTLY_USED);
            }
        }
    }
}
