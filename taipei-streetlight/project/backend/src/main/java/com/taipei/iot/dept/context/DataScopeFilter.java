package com.taipei.iot.dept.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DataScopeFilter {

    public enum FilterType {
        NONE,
        EXACT,
        HIERARCHY_PREFIX
    }

    private final FilterType type;
    private final String fieldName;
    private final Object value;
    private final String hierarchyPathField;
    private final String hierarchyPathPrefix;

    public static DataScopeFilter none() {
        return DataScopeFilter.builder().type(FilterType.NONE).build();
    }

    public static DataScopeFilter exact(String fieldName, Long deptId) {
        return DataScopeFilter.builder()
                .type(FilterType.EXACT)
                .fieldName(fieldName)
                .value(deptId)
                .build();
    }

    public static DataScopeFilter hierarchyPrefix(String hierarchyPathField, String hierarchyPath) {
        return DataScopeFilter.builder()
                .type(FilterType.HIERARCHY_PREFIX)
                .hierarchyPathField(hierarchyPathField)
                .hierarchyPathPrefix(hierarchyPath)
                .build();
    }
}
