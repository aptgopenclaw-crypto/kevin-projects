package com.taipei.iot.repair.service;

import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.fault.entity.FaultTicket;
import com.taipei.iot.fault.service.FaultTicketService;
import com.taipei.iot.repair.dto.InspectionRecordRequest;
import com.taipei.iot.repair.dto.InspectionRecordResponse;
import com.taipei.iot.repair.dto.InspectionTaskRequest;
import com.taipei.iot.repair.dto.InspectionTaskResponse;
import com.taipei.iot.repair.entity.InspectionRecord;
import com.taipei.iot.repair.entity.InspectionTask;
import com.taipei.iot.repair.enums.InspectionResult;
import com.taipei.iot.repair.enums.InspectionTaskStatus;
import com.taipei.iot.repair.enums.InspectionTaskType;
import com.taipei.iot.repair.repository.InspectionRecordRepository;
import com.taipei.iot.repair.repository.InspectionTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InspectionServiceTest {

    @InjectMocks private InspectionService inspectionService;
    @Mock private InspectionTaskRepository taskRepository;
    @Mock private InspectionRecordRepository recordRepository;
    @Mock private FaultTicketService faultTicketService;

    // ── Task CRUD ──

    @Test
    void createTask_success() {
        InspectionTaskRequest request = InspectionTaskRequest.builder()
                .taskName("忠孝東路巡查")
                .taskType(InspectionTaskType.ONE_TIME)
                .build();
        when(taskRepository.save(any())).thenAnswer(inv -> {
            InspectionTask t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        InspectionTaskResponse result = inspectionService.createTask(request);

        assertNotNull(result);
        assertEquals("忠孝東路巡查", result.getTaskName());
        assertEquals(InspectionTaskStatus.ACTIVE, result.getStatus());
    }

    @Test
    void deactivateTask_success() {
        InspectionTask task = InspectionTask.builder()
                .id(1L).status(InspectionTaskStatus.ACTIVE).build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inspectionService.deactivateTask(1L);

        assertEquals(InspectionTaskStatus.INACTIVE, task.getStatus());
    }

    @Test
    void getTaskById_notFound_throwsException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> inspectionService.getTaskById(99L));
    }

    // ── Record with E13 ──

    @Test
    void createRecord_normal_noFaultTicket() {
        InspectionTask task = InspectionTask.builder().id(1L).build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(recordRepository.save(any())).thenAnswer(inv -> {
            InspectionRecord r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        InspectionRecordRequest request = InspectionRecordRequest.builder()
                .taskId(1L).deviceId(5L)
                .result(InspectionResult.NORMAL)
                .notes("正常").build();

        InspectionRecordResponse result = inspectionService.createRecord(request);

        assertNotNull(result);
        assertNull(result.getFaultTicketId());
        verifyNoInteractions(faultTicketService);
    }

    @Test
    void createRecord_needRepair_createsFaultTicket() {
        InspectionTask task = InspectionTask.builder().id(1L).build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(recordRepository.save(any())).thenAnswer(inv -> {
            InspectionRecord r = inv.getArgument(0);
            if (r.getId() == null) r.setId(10L);
            return r;
        });

        FaultTicket fault = FaultTicket.builder().id(100L).build();
        when(faultTicketService.createFromInspection(eq(5L), eq("燈泡不亮"), eq(10L)))
                .thenReturn(fault);

        InspectionRecordRequest request = InspectionRecordRequest.builder()
                .taskId(1L).deviceId(5L)
                .result(InspectionResult.NEED_REPAIR)
                .notes("燈泡不亮").build();

        InspectionRecordResponse result = inspectionService.createRecord(request);

        assertEquals(100L, result.getFaultTicketId());
        verify(faultTicketService).createFromInspection(5L, "燈泡不亮", 10L);
    }

    @Test
    void createRecord_needRepair_noDevice_skipsFaultTicket() {
        InspectionTask task = InspectionTask.builder().id(1L).build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(recordRepository.save(any())).thenAnswer(inv -> {
            InspectionRecord r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        InspectionRecordRequest request = InspectionRecordRequest.builder()
                .taskId(1L).deviceId(null)
                .result(InspectionResult.NEED_REPAIR)
                .notes("路面損壞").build();

        InspectionRecordResponse result = inspectionService.createRecord(request);

        assertNull(result.getFaultTicketId());
        verifyNoInteractions(faultTicketService);
    }
}
