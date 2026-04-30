package com.taipei.iot.fault.service;

import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.enums.ConnectivityType;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.fault.entity.FaultCorrelation;
import com.taipei.iot.fault.entity.FaultTicket;
import com.taipei.iot.fault.enums.RootCauseType;
import com.taipei.iot.fault.repository.FaultCorrelationRepository;
import com.taipei.iot.fault.repository.FaultTicketRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FaultCorrelationService {

    private final FaultTicketRepository faultTicketRepository;
    private final FaultCorrelationRepository faultCorrelationRepository;
    private final DeviceRepository deviceRepository;

    private static final int CIRCUIT_THRESHOLD = 3;
    private static final int CIRCUIT_WINDOW_MINUTES = 30;

    /**
     * 被動偵測：新工單建立後同步檢查（null-check first, skip if absent）
     */
    @Transactional
    public void detectOnNewTicket(FaultTicket ticket) {
        // 維度 1：同回路近 30 分鐘內 ≥ 3 筆（有回路才偵測）
        if (ticket.getCircuitId() != null) {
            long count = faultTicketRepository.countRecentByCircuit(
                    ticket.getCircuitId(),
                    LocalDateTime.now().minusMinutes(CIRCUIT_WINDOW_MINUTES));
            if (count >= CIRCUIT_THRESHOLD) {
                createCorrelation(RootCauseType.CIRCUIT, ticket.getCircuitId());
            }
        }

        // 維度 2：同 Gateway 下是否已有 GATEWAY 告警（有 parent 才偵測）
        if (ticket.getDeviceId() != null) {
            Device device = deviceRepository.findById(ticket.getDeviceId()).orElse(null);
            if (device != null && device.getParentDeviceId() != null
                    && device.getConnectivityType() == ConnectivityType.GATEWAY) {
                log.debug("Gateway correlation check for device {} parent {}",
                        device.getId(), device.getParentDeviceId());
                // future: merge to existing gateway correlation
            }
        }
    }

    @Transactional
    public FaultCorrelation createCorrelation(RootCauseType rootCauseType, Long rootCauseId) {
        FaultCorrelation correlation = FaultCorrelation.builder()
                .rootCauseType(rootCauseType)
                .rootCauseId(rootCauseId)
                .affectedCount(0)
                .status("DETECTED")
                .detectedAt(LocalDateTime.now())
                .build();
        return faultCorrelationRepository.save(correlation);
    }
}
