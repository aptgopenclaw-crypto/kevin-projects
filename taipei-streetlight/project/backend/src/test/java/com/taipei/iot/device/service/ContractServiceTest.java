package com.taipei.iot.device.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.device.dto.ContractResponse;
import com.taipei.iot.device.dto.ContractRequest;
import com.taipei.iot.device.entity.Contract;
import com.taipei.iot.device.enums.ContractStatus;
import com.taipei.iot.device.repository.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @InjectMocks private ContractService contractService;
    @Mock private ContractRepository contractRepository;

    private Contract contract;

    @BeforeEach
    void setUp() {
        var auth = new UsernamePasswordAuthenticationToken("user-001", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        contract = Contract.builder().id(1L)
                .contractCode("C-114-001").contractName("路燈維護契約")
                .status(ContractStatus.ACTIVE).build();
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    @Test
    void getById_found() {
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        ContractResponse res = contractService.getById(1L);
        assertEquals("C-114-001", res.getContractCode());
    }

    @Test
    void getById_notFound_throws() {
        when(contractRepository.findById(99L)).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class, () -> contractService.getById(99L));
        assertEquals(ErrorCode.CONTRACT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void create_setsDefaultStatusActive() {
        ContractRequest req = new ContractRequest();
        req.setContractCode("C-NEW");
        req.setContractName("New");

        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> {
            Contract c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });

        ContractResponse res = contractService.create(req);
        assertEquals(ContractStatus.ACTIVE, res.getStatus());
    }

    @Test
    void create_withExplicitStatus() {
        ContractRequest req = new ContractRequest();
        req.setContractCode("C-EXP");
        req.setContractName("Expired");
        req.setStatus(ContractStatus.EXPIRED);

        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> {
            Contract c = inv.getArgument(0);
            c.setId(3L);
            return c;
        });

        ContractResponse res = contractService.create(req);
        assertEquals(ContractStatus.EXPIRED, res.getStatus());
    }

    @Test
    void delete_success() {
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        contractService.delete(1L);
        verify(contractRepository).delete(contract);
    }

    @Test
    void update_success() {
        ContractRequest req = new ContractRequest();
        req.setContractCode("C-UPD");
        req.setContractName("Updated");

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any())).thenReturn(contract);

        contractService.update(1L, req);
        assertEquals("C-UPD", contract.getContractCode());
    }
}
