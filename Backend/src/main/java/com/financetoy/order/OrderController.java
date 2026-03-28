package com.financetoy.order;

import com.financetoy.order.dto.OrderCreateRequest;
import com.financetoy.order.dto.OrderOperatorDetailResponse;
import com.financetoy.order.dto.OrderOperatorSummaryResponse;
import com.financetoy.order.dto.OrderResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping
    public List<OrderOperatorSummaryResponse> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) FailureMode failureMode,
            @RequestParam(defaultValue = "false") boolean problemOnly,
            @RequestParam(defaultValue = "40") int limit
    ) {
        return orderService.getOrders(status, failureMode, problemOnly, limit);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable String orderId) {
        return orderService.getOrder(orderId);
    }

    @GetMapping("/{orderId}/detail")
    public OrderOperatorDetailResponse getOrderDetail(@PathVariable String orderId) {
        return orderService.getOrderDetail(orderId);
    }
}
