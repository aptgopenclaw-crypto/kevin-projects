package com.taipei.iot.repair.enums;

/**
 * 工單附件所屬工單類型（多態 FK）。
 */
public enum TicketType {
    FAULT_TICKET,
    REPAIR_TICKET,
    REPLACEMENT_ORDER
}
