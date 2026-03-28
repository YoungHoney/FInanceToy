package com.financetoy.ledger;

import com.financetoy.common.BaseTimeEntity;
import com.financetoy.common.exception.InsufficientFundsException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "account_balance")
public class AccountBalance extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String externalAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal availableCash;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal reservedCash;

    protected AccountBalance() {
    }

    public AccountBalance(String externalAccountId, BigDecimal availableCash, BigDecimal reservedCash) {
        this.externalAccountId = externalAccountId;
        this.availableCash = scale(availableCash);
        this.reservedCash = scale(reservedCash);
    }

    public void reserve(BigDecimal amount) {
        BigDecimal normalized = scale(amount);
        if (availableCash.compareTo(normalized) < 0) {
            throw new InsufficientFundsException("주문을 reserve 하기 위한 현금이 부족합니다.");
        }
        availableCash = availableCash.subtract(normalized);
        reservedCash = reservedCash.add(normalized);
    }

    public void release(BigDecimal amount) {
        BigDecimal normalized = scale(amount);
        if (reservedCash.compareTo(normalized) < 0) {
            throw new IllegalStateException("release 할 reserved 금액이 부족합니다.");
        }
        reservedCash = reservedCash.subtract(normalized);
        availableCash = availableCash.add(normalized);
    }

    public void settleReserved(BigDecimal amount) {
        BigDecimal normalized = scale(amount);
        if (reservedCash.compareTo(normalized) < 0) {
            throw new IllegalStateException("체결 정산할 reserved 금액이 부족합니다.");
        }
        reservedCash = reservedCash.subtract(normalized);
    }

    public String getExternalAccountId() {
        return externalAccountId;
    }

    public BigDecimal getAvailableCash() {
        return availableCash;
    }

    public BigDecimal getReservedCash() {
        return reservedCash;
    }

    private BigDecimal scale(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
