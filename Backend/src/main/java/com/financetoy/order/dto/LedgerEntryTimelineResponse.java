package com.financetoy.order.dto;

import com.financetoy.ledger.LedgerEntryType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LedgerEntryTimelineResponse(
        String transactionId,
        LedgerEntryType entryType,
        BigDecimal amount,
        BigDecimal availableBalanceAfter,
        BigDecimal reservedBalanceAfter,
        LocalDateTime occurredAt
) {
}
