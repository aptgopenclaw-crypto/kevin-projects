package com.taipei.iot.tender.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.tender.dto.MailRecipientRequest;
import com.taipei.iot.tender.dto.MailRecipientResponse;
import com.taipei.iot.tender.entity.TenderMailRecipient;
import com.taipei.iot.tender.repository.TenderMailRecipientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailRecipientServiceTest {

    @InjectMocks
    private MailRecipientService service;

    @Mock
    private TenderMailRecipientRepository repository;

    @Test
    void listAll_shouldReturnActiveRecipients() {
        TenderMailRecipient entity = TenderMailRecipient.builder()
                .id(1L).email("test@example.com").name("Test").isActive(true).build();
        when(repository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(entity));

        List<MailRecipientResponse> result = service.listAll();

        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).getEmail());
    }

    @Test
    void getActiveEmails_shouldReturnEmailList() {
        TenderMailRecipient e1 = TenderMailRecipient.builder()
                .id(1L).email("a@example.com").isActive(true).build();
        TenderMailRecipient e2 = TenderMailRecipient.builder()
                .id(2L).email("b@example.com").isActive(true).build();
        when(repository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(e1, e2));

        List<String> emails = service.getActiveEmails();

        assertEquals(List.of("a@example.com", "b@example.com"), emails);
    }

    @Test
    void create_shouldSaveAndReturn() {
        MailRecipientRequest req = new MailRecipientRequest();
        req.setEmail("  Test@Example.COM  ");
        req.setName(" John ");
        req.setIsActive(true);

        TenderMailRecipient saved = TenderMailRecipient.builder()
                .id(1L).email("test@example.com").name("John").isActive(true).build();
        when(repository.save(any())).thenReturn(saved);

        MailRecipientResponse result = service.create(req);

        assertEquals(1L, result.getId());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void create_duplicateEmail_shouldThrowBusinessException() {
        MailRecipientRequest req = new MailRecipientRequest();
        req.setEmail("dup@example.com");
        req.setIsActive(true);

        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("unique constraint"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertEquals(ErrorCode.MAIL_RECIPIENT_EMAIL_DUPLICATE, ex.getErrorCode());
    }

    @Test
    void update_existingId_shouldUpdateFields() {
        TenderMailRecipient existing = TenderMailRecipient.builder()
                .id(1L).email("old@example.com").name("Old").isActive(true).build();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenReturn(existing);

        MailRecipientRequest req = new MailRecipientRequest();
        req.setEmail("new@example.com");
        req.setName("New");
        req.setIsActive(false);

        MailRecipientResponse result = service.update(1L, req);

        assertEquals("new@example.com", existing.getEmail());
        assertEquals("New", existing.getName());
        assertFalse(existing.getIsActive());
    }

    @Test
    void update_nonExistingId_shouldThrow() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        MailRecipientRequest req = new MailRecipientRequest();
        req.setEmail("x@example.com");

        assertThrows(IllegalArgumentException.class, () -> service.update(99L, req));
    }

    @Test
    void delete_existingId_shouldCallRepository() {
        when(repository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void delete_nonExistingId_shouldThrow() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.delete(99L));
    }
}
