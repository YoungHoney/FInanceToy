package com.financetoy.order;

public enum OrderStatus {
    CREATED,
    RESERVED,
    EXECUTING,
    EXECUTED,
    CANCELLED,
    COMPENSATED,
    RECONCILE_REQUIRED
}
