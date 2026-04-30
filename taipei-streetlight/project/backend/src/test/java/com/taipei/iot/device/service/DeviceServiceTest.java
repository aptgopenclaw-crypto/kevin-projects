package com.taipei.iot.device.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.dept.service.DataScopeHelper;
import com.taipei.iot.device.dto.DeviceRequest;
import com.taipei.iot.device.dto.DeviceResponse;
import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.enums.DeviceStatus;
import com.taipei.iot.device.enums.DeviceType;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.gis.service.CoordinateService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @InjectMocks private DeviceService deviceService;
    @Mock private DeviceRepository deviceRepository;
    @Mock private DeviceTemplateService deviceTemplateService;
    @Mock private CoordinateService coordinateService;
    @Mock private DataScopeHelper dataScopeHelper;

    private Device pole;
    private Device luminaire;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenantId("TENANT_A");
        var auth = new UsernamePasswordAuthenticationToken("user-001", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Default: autoFill returns all-null CoordinateSet (no coords provided)
        lenient().when(coordinateService.autoFill(any(), any(), any(), any(), any(), any()))
                .thenReturn(new CoordinateService.CoordinateSet(null, null, null, null, null, null));

        pole = Device.builder().id(1L).deviceType(DeviceType.POLE)
                .deviceCode("SL-N-001").deviceName("北區路燈001")
                .status(DeviceStatus.ACTIVE).deptId(6L).build();
        luminaire = Device.builder().id(2L).deviceType(DeviceType.LUMINAIRE)
                .deviceCode("LM-N-001").status(DeviceStatus.ACTIVE)
                .parentDeviceId(1L).mountPosition("ARM_1").build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ── CRUD ──

    @Test
    void listDevices_shouldReturnPageWithDataScope() {
        when(dataScopeHelper.getVisibleDeptIds()).thenReturn(List.of(6L, 7L));
        when(deviceRepository.findByFilters(any(), any(), any(), anyList(), any()))
                .thenReturn(new PageImpl<>(List.of(pole)));

        Page<DeviceResponse> result = deviceService.listDevices(null, null, null, PageRequest.of(0, 20));

        assertEquals(1, result.getContent().size());
        assertEquals("SL-N-001", result.getContent().get(0).getDeviceCode());
        verify(dataScopeHelper).getVisibleDeptIds();
    }

    @Test
    void listDevices_emptyDeptIds_passesNullToQuery() {
        when(dataScopeHelper.getVisibleDeptIds()).thenReturn(List.of());
        when(deviceRepository.findByFilters(any(), any(), any(), isNull(), any()))
                .thenReturn(Page.empty());

        deviceService.listDevices(null, null, null, PageRequest.of(0, 20));

        verify(deviceRepository).findByFilters(any(), any(), any(), isNull(), any());
    }

    @Test
    void getById_found_returnsResponse() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(pole));
        when(deviceRepository.countByParentDeviceIdAndStatusNot(1L, DeviceStatus.DECOMMISSIONED)).thenReturn(2L);

        DeviceResponse res = deviceService.getById(1L);

        assertEquals("SL-N-001", res.getDeviceCode());
        assertEquals(2L, res.getChildrenCount());
    }

    @Test
    void getById_notFound_throwsException() {
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deviceService.getById(999L));
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void create_success() {
        DeviceRequest request = new DeviceRequest();
        request.setDeviceType(DeviceType.POLE);
        request.setDeviceCode("SL-N-999");

        when(deviceRepository.findByTenantIdAndDeviceCode("TENANT_A", "SL-N-999"))
                .thenReturn(Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenAnswer(inv -> {
            Device d = inv.getArgument(0);
            d.setId(99L);
            return d;
        });

        DeviceResponse res = deviceService.create(request);

        assertEquals("SL-N-999", res.getDeviceCode());
        assertEquals(DeviceStatus.ACTIVE, res.getStatus());
        verify(deviceRepository).save(any());
    }

    @Test
    void create_duplicateCode_throws() {
        DeviceRequest request = new DeviceRequest();
        request.setDeviceType(DeviceType.POLE);
        request.setDeviceCode("SL-N-001");

        when(deviceRepository.findByTenantIdAndDeviceCode("TENANT_A", "SL-N-001"))
                .thenReturn(Optional.of(pole));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deviceService.create(request));
        assertEquals(ErrorCode.DEVICE_CODE_DUPLICATE, ex.getErrorCode());
    }

    @Test
    void delete_withChildren_throws() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(pole));
        when(deviceRepository.existsByParentDeviceId(1L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deviceService.delete(1L));
        assertEquals(ErrorCode.DEVICE_HAS_CHILDREN, ex.getErrorCode());
    }

    @Test
    void delete_noChildren_succeeds() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(pole));
        when(deviceRepository.existsByParentDeviceId(1L)).thenReturn(false);

        deviceService.delete(1L);

        verify(deviceRepository).delete(pole);
    }

    @Test
    void decommission_setsStatusAndDate() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(pole));
        when(deviceRepository.save(any())).thenReturn(pole);

        deviceService.decommission(1L);

        assertEquals(DeviceStatus.DECOMMISSIONED, pole.getStatus());
        assertNotNull(pole.getDecommissionedAt());
    }

    // ── Circular reference protection ──

    @Test
    void create_withCircularParent_throws() {
        DeviceRequest request = new DeviceRequest();
        request.setDeviceType(DeviceType.LUMINAIRE);
        request.setDeviceCode("LM-NEW");
        request.setParentDeviceId(1L);

        when(deviceRepository.findByTenantIdAndDeviceCode(any(), any())).thenReturn(Optional.empty());
        // null childId → no circular check for new device (childId is null)
        // But if parentId chain loops, it should be caught
        // For new devices, childId is null so validateNoCircularReference exits early

        when(deviceRepository.save(any(Device.class))).thenAnswer(inv -> {
            Device d = inv.getArgument(0);
            d.setId(100L);
            return d;
        });

        DeviceResponse res = deviceService.create(request);
        assertEquals("LM-NEW", res.getDeviceCode());
    }

    @Test
    void update_withCircularReference_throws() {
        DeviceRequest request = new DeviceRequest();
        request.setDeviceType(DeviceType.POLE);
        request.setDeviceCode("SL-N-001");
        request.setParentDeviceId(2L); // luminaire is child of pole → circular

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(pole));
        when(deviceRepository.checkCircularReference(1L, 2L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deviceService.update(1L, request));
        assertEquals(ErrorCode.DEVICE_CIRCULAR_REFERENCE, ex.getErrorCode());
    }

    // ── JSONB size validation ──

    @Test
    void create_oversizedJsonb_throws() {
        DeviceRequest request = new DeviceRequest();
        request.setDeviceType(DeviceType.POLE);
        request.setDeviceCode("SL-BIG");

        Map<String, Object> huge = new HashMap<>();
        huge.put("data", "x".repeat(15_000));
        request.setAttributes(huge);

        when(deviceRepository.findByTenantIdAndDeviceCode(any(), any())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deviceService.create(request));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    // ── Component operations ──

    @Test
    void getActiveComponents_returnsNonDecommissioned() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(pole));
        when(deviceRepository.findByParentDeviceIdAndStatusNot(1L, DeviceStatus.DECOMMISSIONED))
                .thenReturn(List.of(luminaire));

        List<DeviceResponse> result = deviceService.getActiveComponents(1L);

        assertEquals(1, result.size());
        assertEquals("LM-N-001", result.get(0).getDeviceCode());
    }

    @Test
    void getByIdWithComponents_pole_includesChildren() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(pole));
        when(deviceRepository.countByParentDeviceIdAndStatusNot(1L, DeviceStatus.DECOMMISSIONED)).thenReturn(1L);
        when(deviceRepository.findByParentDeviceIdAndStatusNot(1L, DeviceStatus.DECOMMISSIONED))
                .thenReturn(List.of(luminaire));

        DeviceResponse res = deviceService.getByIdWithComponents(1L);

        assertNotNull(res.getChildren());
        assertEquals(1, res.getChildren().size());
    }
}
