package com.financetoy.ledger;

import com.financetoy.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "ledger_entry")
public class LedgerEntry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String transactionId;

    @Column(nullable = false, length = 100)
    private String orderId;

    @Column(nullable = false, length = 100)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LedgerEntryType entryType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal availableBalanceAfter;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal reservedBalanceAfter;

    protected LedgerEntry() {
    }

    public LedgerEntry(
            String transactionId,
            String orderId,
            String accountId,
            LedgerEntryType entryType,
            BigDecimal amount,
            BigDecimal availableBalanceAfter,
            BigDecimal reservedBalanceAfter
    ) {
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.accountId = accountId;
        this.entryType = entryType;
        this.amount = amount;
        this.availableBalanceAfter = availableBalanceAfter;
        this.reservedBalanceAfter = reservedBalanceAfter;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getAccountId() {
        return accountId;
    }

    public LedgerEntryType getEntryType() {
        return entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getAvailableBalanceAfter() {
        return availableBalanceAfter;
    }

    public BigDecimal getReservedBalanceAfter() {
        return reservedBalanceAfter;
    }
}
