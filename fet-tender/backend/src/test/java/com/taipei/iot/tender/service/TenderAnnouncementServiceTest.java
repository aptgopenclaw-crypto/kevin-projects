package com.taipei.iot.tender.service;

import com.taipei.iot.tender.dto.TenderAnnouncementQueryRequest;
import com.taipei.iot.tender.dto.TenderAnnouncementResponse;
import com.taipei.iot.tender.entity.TenderAnnouncement;
import com.taipei.iot.tender.repository.TenderAnnouncementRepository;
import com.taipei.iot.common.dto.PageResponse;
import com.taipei.iot.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenderAnnouncementServiceTest {

    @InjectMocks
    private TenderAnnouncementService service;

    @Mock
    private TenderAnnouncementRepository repository;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenantId("TENANT_TEST");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void search_shouldReturnPageResponse() {
        TenderAnnouncement entity = TenderAnnouncement.builder()
                .id(1L).solution("IoT").matchedKeyword("智慧路燈")
                .agencyName("台北市政府").tenderNumber("T-001")
                .tenderName("智慧路燈案").announcementDate(LocalDate.of(2026, 5, 1))
                .build();
        Page<TenderAnnouncement> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1);
        when(repository.search(any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        TenderAnnouncementQueryRequest req = new TenderAnnouncementQueryRequest();
        req.setPage(0);
        req.setSize(20);

        PageResponse<TenderAnnouncementResponse> result = service.search(req);

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("T-001", result.getContent().get(0).getTenderNumber());
    }

    @Test
    void getById_existingId_shouldReturnResponse() {
        TenderAnnouncement entity = TenderAnnouncement.builder()
                .id(1L).solution("IoT").matchedKeyword("智慧")
                .tenderNumber("T-002").tenderName("Test")
                .announcementDate(LocalDate.of(2026, 5, 1))
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        TenderAnnouncementResponse result = service.getById(1L);

        assertEquals("T-002", result.getTenderNumber());
    }

    @Test
    void getById_nonExistingId_shouldThrow() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getById(99L));
    }

    @Test
    void deleteById_existingId_shouldCallRepository() {
        when(repository.existsById(1L)).thenReturn(true);

        service.deleteById(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteById_nonExistingId_shouldThrow() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.deleteById(99L));
    }
}
