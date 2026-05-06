package com.taipei.iot.audit.job;

import com.taipei.iot.audit.repository.UserEventLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditPurgeJobTest {

    @Mock
    private UserEventLogRepository userEventLogRepository;

    @InjectMocks
    private AuditPurgeJob auditPurgeJob;

    @Test
    void purgeOldAuditLogs_shouldDeleteRecordsOlderThan7Days() {
        when(userEventLogRepository.deleteByCreateTimeBefore(any(LocalDateTime.class)))
                .thenReturn(100);

        auditPurgeJob.purgeOldAuditLogs();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userEventLogRepository).deleteByCreateTimeBefore(captor.capture());

        LocalDateTime cutoff = captor.getValue();
        // cutoff should be approximately 7 days ago
        assertTrue(cutoff.isBefore(LocalDateTime.now().minusDays(6)));
        assertTrue(cutoff.isAfter(LocalDateTime.now().minusDays(8)));
    }

    @Test
    void purgeOldAuditLogs_shouldHandleZeroDeleted() {
        when(userEventLogRepository.deleteByCreateTimeBefore(any(LocalDateTime.class)))
                .thenReturn(0);

        assertDoesNotThrow(() -> auditPurgeJob.purgeOldAuditLogs());
        verify(userEventLogRepository).deleteByCreateTimeBefore(any(LocalDateTime.class));
    }
}
