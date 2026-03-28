package com.financetoy.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_event")
public class OrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderEventType eventType;

    @Column(nullable = false, length = 500)
    private String detail;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    protected OrderEvent() {
    }

    public OrderEvent(String orderId, OrderEventType eventType, String detail) {
        this.orderId = orderId;
        this.eventType = eventType;
        this.detail = detail;
        this.occurredAt = LocalDateTime.now();
    }
}
