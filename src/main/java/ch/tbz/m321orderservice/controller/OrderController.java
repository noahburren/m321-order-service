package ch.tbz.m321orderservice.controller;

import ch.tbz.m321orderservice.api.CreateOrderV1Request;
import ch.tbz.m321orderservice.api.CreateOrderV2Request;
import ch.tbz.m321orderservice.api.OrderAcceptedResponse;
import ch.tbz.m321orderservice.observability.CorrelationIdFilter;
import ch.tbz.m321orderservice.service.OrderApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderApplicationService orderService;

    public OrderController(OrderApplicationService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/v1/orders")
    public ResponseEntity<OrderAcceptedResponse> createV1(
            @Valid @RequestBody CreateOrderV1Request request,
            @RequestAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE) String correlationId) {
        return ResponseEntity.accepted()
                .body(orderService.accept(request.product(), request.quantity(), correlationId));
    }

    @PostMapping("/v2/orders")
    public ResponseEntity<OrderAcceptedResponse> createV2(
            @Valid @RequestBody CreateOrderV2Request request,
            @RequestAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE) String correlationId) {
        return ResponseEntity.accepted()
                .body(orderService.accept(request.productCode(), request.amount(), correlationId));
    }
}
