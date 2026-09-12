package com.microservices.orderservice.service;

import com.microservices.orderservice.dto.*;
import com.microservices.orderservice.model.Order;
import com.microservices.orderservice.model.OrderLineItems;
import com.microservices.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void placeOrder(OrderRequest orderRequest) {
        // Create order with random ID
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        // Map DTOs to entities
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToEntity)
                .toList();
        order.setOrderLineItemsList(orderLineItems);



        List<InventoryRequest> requests = order.getOrderLineItemsList().stream()
                .map(item -> new InventoryRequest(item.getSkuCode(), item.getQuantity()))
                .toList();

        boolean stockAvailable = Boolean.TRUE.equals(webClientBuilder.build().post()
                .uri("http://inventory-service/api/inventory/check")
                .bodyValue(requests)
                .retrieve()
                .bodyToMono(Boolean.class)
                .block());

        if (stockAvailable) {
            log.info("Before Saving Order Repo + {}", order.getOrderNumber());
            orderRepository.save(order);
            OrderPlacedEvent orderPlacedEvent = new OrderPlacedEvent();
            orderPlacedEvent.setOrderNumber(order.getOrderNumber());
            kafkaTemplate.send("notificationTopic", orderPlacedEvent);
            log.info("Processing order number + {}", order.getOrderNumber());
        } else {
            throw new IllegalArgumentException("One or more products are out of stock, please try again later");
        }
    }

    private OrderLineItems mapToEntity(OrderLineItemsDto dto) {
        OrderLineItems entity = new OrderLineItems();
        entity.setPrice(dto.getPrice());
        entity.setQuantity(dto.getQuantity());
        entity.setSkuCode(dto.getSkuCode());
        return entity;
    }


}
