package com.taipei.iot.replacement.service;

import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.device.dto.DeviceResponse;
import com.taipei.iot.device.service.DeviceService;
import com.taipei.iot.material.entity.ApprovedMaterial;
import com.taipei.iot.material.enums.ApprovedMaterialStatus;
import com.taipei.iot.material.repository.ApprovedMaterialRepository;
import com.taipei.iot.replacement.dto.ReplacementItemRequest;
import com.taipei.iot.replacement.dto.ReplacementItemResponse;
import com.taipei.iot.replacement.entity.ReplacementItem;
import com.taipei.iot.replacement.entity.ReplacementOrder;
import com.taipei.iot.replacement.enums.ReplacementItemStatus;
import com.taipei.iot.replacement.enums.ReplacementOrderStatus;
import com.taipei.iot.replacement.repository.ReplacementItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReplacementItemServiceTest {

    @InjectMocks private ReplacementItemService service;
    @Mock private ReplacementItemRepository itemRepo;
    @Mock private ReplacementOrderService orderService;
    @Mock private DeviceService deviceService;
    @Mock private ApprovedMaterialRepository approvedMaterialRepo;

    @Test
    void addItem_success() {
        ReplacementOrder order = ReplacementOrder.builder()
                .id(1L).status(ReplacementOrderStatus.DRAFT).build();
        when(orderService.findOrThrow(1L)).thenReturn(order);

        DeviceResponse parentDevice = DeviceResponse.builder().id(100L).deviceCode("P-001").build();
        DeviceResponse oldDevice = DeviceResponse.builder().id(200L).parentDeviceId(100L).deviceType(null).deviceCode("L-001").build();
        when(deviceService.getById(100L)).thenReturn(parentDevice);
        when(deviceService.getById(200L)).thenReturn(oldDevice);

        when(itemRepo.save(any())).thenAnswer(inv -> {
            ReplacementItem item = inv.getArgument(0);
            item.setId(10L);
            return item;
        });

        ReplacementItemRequest request = ReplacementItemRequest.builder()
                .parentDeviceId(100L).oldDeviceId(200L).build();

        ReplacementItemResponse result = service.addItem(1L, request);

        assertNotNull(result);
        assertEquals(ReplacementItemStatus.PENDING, result.getStatus());
        verify(itemRepo).save(any());
    }

    @Test
    void addItem_oldDeviceNotBelongToParent_throwsException() {
        ReplacementOrder order = ReplacementOrder.builder()
                .id(1L).status(ReplacementOrderStatus.DRAFT).build();
        when(orderService.findOrThrow(1L)).thenReturn(order);

        DeviceResponse parentDevice = DeviceResponse.builder().id(100L).build();
        DeviceResponse oldDevice = DeviceResponse.builder().id(200L).parentDeviceId(999L).build();
        when(deviceService.getById(100L)).thenReturn(parentDevice);
        when(deviceService.getById(200L)).thenReturn(oldDevice);

        assertThrows(BusinessException.class,
                () -> service.addItem(1L, ReplacementItemRequest.builder()
                        .parentDeviceId(100L).oldDeviceId(200L).build()));
    }

    @Test
    void addItem_materialNotActive_throwsException() {
        ReplacementOrder order = ReplacementOrder.builder()
                .id(1L).status(ReplacementOrderStatus.DRAFT).build();
        when(orderService.findOrThrow(1L)).thenReturn(order);

        DeviceResponse parentDevice = DeviceResponse.builder().id(100L).build();
        DeviceResponse oldDevice = DeviceResponse.builder().id(200L).parentDeviceId(100L).build();
        when(deviceService.getById(100L)).thenReturn(parentDevice);
        when(deviceService.getById(200L)).thenReturn(oldDevice);

        ApprovedMaterial expired = new ApprovedMaterial();
        expired.setStatus(ApprovedMaterialStatus.EXPIRED);
        when(approvedMaterialRepo.findById(50L)).thenReturn(Optional.of(expired));

        assertThrows(BusinessException.class,
                () -> service.addItem(1L, ReplacementItemRequest.builder()
                        .parentDeviceId(100L).oldDeviceId(200L).approvedMaterialId(50L).build()));
    }

    @Test
    void addItem_materialNotFound_throwsException() {
        ReplacementOrder order = ReplacementOrder.builder()
                .id(1L).status(ReplacementOrderStatus.DRAFT).build();
        when(orderService.findOrThrow(1L)).thenReturn(order);

        DeviceResponse parentDevice = DeviceResponse.builder().id(100L).build();
        DeviceResponse oldDevice = DeviceResponse.builder().id(200L).parentDeviceId(100L).build();
        when(deviceService.getById(100L)).thenReturn(parentDevice);
        when(deviceService.getById(200L)).thenReturn(oldDevice);

        when(approvedMaterialRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> service.addItem(1L, ReplacementItemRequest.builder()
                        .parentDeviceId(100L).oldDeviceId(200L).approvedMaterialId(99L).build()));
    }

    @Test
    void addItem_invalidStatus_throwsException() {
        ReplacementOrder order = ReplacementOrder.builder()
                .id(1L).status(ReplacementOrderStatus.CLOSED).build();
        when(orderService.findOrThrow(1L)).thenReturn(order);

        assertThrows(BusinessException.class,
                () -> service.addItem(1L, ReplacementItemRequest.builder()
                        .parentDeviceId(100L).oldDeviceId(200L).build()));
    }

    @Test
    void getItems_success() {
        ReplacementItem item = ReplacementItem.builder()
                .id(10L).orderId(1L).parentDeviceId(100L).oldDeviceId(200L)
                .status(ReplacementItemStatus.PENDING).build();
        when(itemRepo.findByOrderId(1L)).thenReturn(List.of(item));

        List<ReplacementItemResponse> result = service.getItems(1L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
    }

    @Test
    void deleteItem_draftStatus_success() {
        ReplacementOrder order = ReplacementOrder.builder()
                .id(1L).status(ReplacementOrderStatus.DRAFT).build();
        when(orderService.findOrThrow(1L)).thenReturn(order);

        service.deleteItem(1L, 10L);

        verify(itemRepo).deleteById(10L);
    }

    @Test
    void deleteItem_nonDraftStatus_throwsException() {
        ReplacementOrder order = ReplacementOrder.builder()
                .id(1L).status(ReplacementOrderStatus.IN_PROGRESS).build();
        when(orderService.findOrThrow(1L)).thenReturn(order);

        assertThrows(BusinessException.class,
                () -> service.deleteItem(1L, 10L));
    }
}
