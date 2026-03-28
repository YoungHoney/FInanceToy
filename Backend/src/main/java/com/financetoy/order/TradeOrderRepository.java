package com.financetoy.order;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {

    Optional<TradeOrder> findByOrderId(String orderId);

    Optional<TradeOrder> findByIdempotencyKey(String idempotencyKey);

    List<TradeOrder> findAllByOrderByCreatedAtDesc();

    List<TradeOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<TradeOrder> findByFailureModeOrderByCreatedAtDesc(FailureMode failureMode);

    List<TradeOrder> findByStatusAndFailureModeOrderByCreatedAtDesc(OrderStatus status, FailureMode failureMode);
}
