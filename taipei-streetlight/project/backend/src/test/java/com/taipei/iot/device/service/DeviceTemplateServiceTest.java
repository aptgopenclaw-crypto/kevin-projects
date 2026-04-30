package com.taipei.iot.device.service;

import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.device.entity.DeviceTemplate;
import com.taipei.iot.device.repository.DeviceTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceTemplateServiceTest {

    @Mock
    private DeviceTemplateRepository templateRepository;

    @InjectMocks
    private DeviceTemplateService service;

    private Map<String, Object> buildSchema(List<Map<String, Object>> fields) {
        return Map.of("fields", fields);
    }

    // ── getSchema ──

    @Test
    void getSchema_exists_returnsSchema() {
        DeviceTemplate tpl = new DeviceTemplate();
        tpl.setSchema(Map.of("fields", List.of()));
        when(templateRepository.findByDeviceType("POLE")).thenReturn(Optional.of(tpl));

        var result = service.getSchema("POLE");
        assertThat(result).containsKey("fields");
    }

    @Test
    void getSchema_notExists_returnsNull() {
        when(templateRepository.findByDeviceType("UNKNOWN")).thenReturn(Optional.empty());
        assertThat(service.getSchema("UNKNOWN")).isNull();
    }

    // ── validateAttributes: no schema = pass ──

    @Test
    void validateAttributes_noSchema_passesThrough() {
        when(templateRepository.findByDeviceType("POLE")).thenReturn(Optional.empty());
        assertThatCode(() -> service.validateAttributes("POLE", Map.of("any", "value")))
                .doesNotThrowAnyException();
    }

    // ── required check ──

    @Test
    void validateAttributes_requiredFieldMissing_throws() {
        var field = Map.<String, Object>of("key", "height", "title", "桿高", "type", "number", "required", true);
        DeviceTemplate tpl = new DeviceTemplate();
        tpl.setSchema(buildSchema(List.of(field)));
        when(templateRepository.findByDeviceType("POLE")).thenReturn(Optional.of(tpl));

        assertThatThrownBy(() -> service.validateAttributes("POLE", Map.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("必填");
    }

    @Test
    void validateAttributes_requiredFieldPresent_passes() {
        var field = Map.<String, Object>of("key", "height", "title", "桿高", "type", "number", "required", true);
        DeviceTemplate tpl = new DeviceTemplate();
        tpl.setSchema(buildSchema(List.of(field)));
        when(templateRepository.findByDeviceType("POLE")).thenReturn(Optional.of(tpl));

        assertThatCode(() -> service.validateAttributes("POLE", Map.of("height", 10)))
                .doesNotThrowAnyException();
    }

    // ── type validation ──

    @Test
    void validateAttributes_numberField_wrongType_throws() {
        var field = Map.<String, Object>of("key", "height", "title", "桿高", "type", "number");
        DeviceTemplate tpl = new DeviceTemplate();
        tpl.setSchema(buildSchema(List.of(field)));
        when(templateRepository.findByDeviceType("POLE")).thenReturn(Optional.of(tpl));

        assertThatThrownBy(() -> service.validateAttributes("POLE", Map.of("height", "abc")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("數字");
    }

    @Test
    void validateAttributes_numberField_outOfRange_throws() {
        var field = Map.<String, Object>of(
                "key", "height", "title", "桿高", "type", "number",
                "minimum", 3, "maximum", 20);
        DeviceTemplate tpl = new DeviceTemplate();
        tpl.setSchema(buildSchema(List.of(field)));
        when(templateRepository.findByDeviceType("POLE")).thenReturn(Optional.of(tpl));

        assertThatThrownBy(() -> service.validateAttributes("POLE", Map.of("height", 25)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可大於");
    }

    @Test
    void validateAttributes_selectField_invalidOption_throws() {
        var field = Map.of(
                "key", (Object) "lampType",
                "title", (Object) "燈具型式",
                "type", (Object) "select",
                "options", List.of("LED", "HPS"));
        DeviceTemplate tpl = new DeviceTemplate();
        tpl.setSchema(buildSchema(List.of(field)));
        when(templateRepository.findByDeviceType("LUMINAIRE")).thenReturn(Optional.of(tpl));

        assertThatThrownBy(() -> service.validateAttributes("LUMINAIRE", Map.of("lampType", "NEON")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不在允許選項中");
    }

    // ── extra keys preserved (open schema) ──

    @Test
    void validateAttributes_extraKeys_notRejected() {
        var field = Map.<String, Object>of("key", "height", "title", "桿高", "type", "number");
        DeviceTemplate tpl = new DeviceTemplate();
        tpl.setSchema(buildSchema(List.of(field)));
        when(templateRepository.findByDeviceType("POLE")).thenReturn(Optional.of(tpl));

        assertThatCode(() -> service.validateAttributes("POLE",
                Map.of("height", 10, "customField", "any")))
                .doesNotThrowAnyException();
    }

    @Test
    void validateAttributes_checkboxField_wrongType_throws() {
        var field = Map.<String, Object>of("key", "hasSensor", "title", "有感測器", "type", "checkbox");
        DeviceTemplate tpl = new DeviceTemplate();
        tpl.setSchema(buildSchema(List.of(field)));
        when(templateRepository.findByDeviceType("POLE")).thenReturn(Optional.of(tpl));

        assertThatThrownBy(() -> service.validateAttributes("POLE", Map.of("hasSensor", "yes")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("布林值");
    }
}
