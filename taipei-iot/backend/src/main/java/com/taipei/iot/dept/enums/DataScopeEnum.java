package com.taipei.iot.dept.enums;

public enum DataScopeEnum {
    ALL,
    THIS_LEVEL,
    THIS_LEVEL_AND_BELOW;

    public static DataScopeEnum fromString(String value) {
        if (value == null) {
            return ALL;
        }
        try {
            return DataScopeEnum.valueOf(value);
        } catch (IllegalArgumentException e) {
            return ALL;
        }
    }
}
