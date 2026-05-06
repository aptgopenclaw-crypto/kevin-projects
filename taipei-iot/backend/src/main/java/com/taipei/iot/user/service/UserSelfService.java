package com.taipei.iot.user.service;

import com.taipei.iot.auth.entity.ChangePasswordLogEntity;
import com.taipei.iot.auth.entity.UserEntity;
import com.taipei.iot.auth.repository.ChangePasswordLogRepository;
import com.taipei.iot.auth.repository.UserRepository;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.user.dto.request.ChangePasswordRequest;
import com.taipei.iot.user.dto.request.UpdateOwnProfileRequest;
import com.taipei.iot.user.entity.PasswordHistoryEntity;
import com.taipei.iot.user.repository.PasswordHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSelfService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final ChangePasswordLogRepository changePasswordLogRepository;
    private final UserAuditService userAuditService;

    @Transactional
    public UserEntity updateOwnProfile(String currentUserId, UpdateOwnProfileRequest req) {
        UserEntity user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (req.getDisplayName() != null) {
            user.setDisplayName(req.getDisplayName());
        }
        if (req.getPhone() != null) {
            user.setPhone(req.getPhone());
        }

        UserEntity saved = userRepository.save(user);
        userAuditService.logAction("UPDATE", currentUserId, currentUserId, "自助更新個人資料");
        return saved;
    }

    @Transactional
    public void changePassword(String currentUserId, ChangePasswordRequest req) {
        UserEntity user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        passwordValidator.validate(req.getNewPassword());
        passwordValidator.checkNotRecentlyUsed(currentUserId, req.getNewPassword());

        String newHash = passwordEncoder.encode(req.getNewPassword());
        user.setPasswordHash(newHash);
        userRepository.save(user);

        passwordHistoryRepository.save(PasswordHistoryEntity.builder()
                .userId(currentUserId)
                .passwordHash(newHash)
                .build());

        changePasswordLogRepository.save(ChangePasswordLogEntity.builder()
                .userId(currentUserId)
                .changeType("SELF_CHANGE")
                .build());

        userAuditService.logAction("UPDATE", currentUserId, currentUserId, "自助修改密碼");
    }
}
