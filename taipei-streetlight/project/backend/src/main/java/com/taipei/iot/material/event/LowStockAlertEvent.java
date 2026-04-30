package com.taipei.iot.material.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class LowStockAlertEvent extends ApplicationEvent {

    private final String tenantId;
    private final String specName;
    private final String warehouseName;
    private final int quantityOnHand;
    private final int safetyStock;

    public LowStockAlertEvent(String tenantId, String specName, String warehouseName,
                               int quantityOnHand, int safetyStock) {
        super(tenantId);
        this.tenantId = tenantId;
        this.specName = specName;
        this.warehouseName = warehouseName;
        this.quantityOnHand = quantityOnHand;
        this.safetyStock = safetyStock;
    }
}
