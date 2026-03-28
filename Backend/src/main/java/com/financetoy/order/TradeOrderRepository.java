package com.financetoy.order;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {

    Optional<TradeOrder> findByOrderId(String orderId);

    Optional<TradeOrder> findByIdempotencyKey(String idempotencyKey);
}
