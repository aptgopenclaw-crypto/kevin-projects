package com.taipei.iot.notification.channel;

import com.taipei.iot.auth.entity.UserEntity;
import com.taipei.iot.auth.repository.UserRepository;
import com.taipei.iot.notification.dto.NotificationPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class EmailChannel implements NotificationChannel {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Override
    public String channelType() {
        return "EMAIL";
    }

    @Override
    public void send(NotificationPayload payload) {
        for (String userId : payload.getUserIds()) {
            userRepository.findById(userId).ifPresent(user -> {
                if (Boolean.TRUE.equals(user.getNotifyEmailFlag()) && user.getEmail() != null) {
                    sendEmail(user, payload);
                }
            });
        }
    }

    private void sendEmail(UserEntity user, NotificationPayload payload) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            String safeTitle = sanitize(payload.getTitle());
            message.setSubject("[路燈平台] " + safeTitle);
            String content = payload.getContent() != null ? payload.getContent() : safeTitle;
            message.setText(sanitize(content));
            mailSender.send(message);
            log.info("Email sent to userId={}", user.getUserId());
        } catch (Exception e) {
            log.warn("Failed to send email to userId={}: {}", user.getUserId(), e.getMessage());
        }
    }

    private String sanitize(String input) {
        if (input == null) return "";
        return input.replaceAll("[\\r\\n]", " ");
    }
}
