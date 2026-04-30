package com.taipei.iot.dept.context;

public final class DataScopeContext {

    private static final ThreadLocal<DataScopeFilter> CONTEXT = new ThreadLocal<>();

    private DataScopeContext() {}

    public static void set(DataScopeFilter filter) {
        CONTEXT.set(filter);
    }

    public static DataScopeFilter get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
