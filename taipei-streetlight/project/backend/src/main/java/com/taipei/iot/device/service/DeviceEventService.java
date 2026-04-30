package com.taipei.iot.device.service;

import com.taipei.iot.device.entity.DeviceEvent;
import com.taipei.iot.device.enums.DeviceEventType;
import com.taipei.iot.device.repository.DeviceEventRepository;
import com.taipei.iot.common.util.SecurityContextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceEventService {

    private final DeviceEventRepository deviceEventRepository;

    public Page<DeviceEvent> getByDeviceId(Long deviceId, Pageable pageable) {
        return deviceEventRepository.findByDeviceIdOrderByEventDateDesc(deviceId, pageable);
    }

    @Transactional
    public DeviceEvent recordEvent(Long deviceId, DeviceEventType eventType,
                                   String description, List<Map<String, Object>> attachments) {
        return recordEvent(deviceId, eventType, description, attachments, null, null);
    }

    @Transactional
    public DeviceEvent recordEvent(Long deviceId, DeviceEventType eventType,
                                   String description, List<Map<String, Object>> attachments,
                                   Long repairTicketId, Long replacementItemId) {
        DeviceEvent event = DeviceEvent.builder()
                .deviceId(deviceId)
                .eventType(eventType)
                .eventDate(LocalDateTime.now())
                .description(description)
                .attachments(attachments)
                .repairTicketId(repairTicketId)
                .replacementItemId(replacementItemId)
                .createdBy(SecurityContextUtils.getCurrentUserId())
                .build();
        return deviceEventRepository.save(event);
    }
}
