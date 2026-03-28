package com.financetoy.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {

    List<OrderEvent> findByOrderIdOrderByOccurredAtAsc(String orderId);

    boolean existsByOrderIdAndEventType(String orderId, OrderEventType eventType);
}
