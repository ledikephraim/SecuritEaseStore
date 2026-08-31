package com.example.store.controller;

import com.example.store.dto.OrderDTO;
import com.example.store.dto.OrderSimpleDTO;
import com.example.store.entity.Order;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 500;

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    /**
     * Get all orders with pagination.
     *
     * @param page zero-based page number
     * @param size page size (default 50, max 500)
     * @param customerId optional customer ID filter
     * @return paginated list of orders
     */
    @GetMapping
    public Object getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE + "") int size,
            @RequestParam(required = false) Long customerId) {

        size = Math.min(size, MAX_PAGE_SIZE);
        size = Math.max(size, 1);

        Pageable pageable = PageRequest.of(page, size);
        Page<Long> orderIds;

        // Customer-filtered orders: return without customer (context already in query param)
        if (customerId != null) {
            orderIds = orderRepository.findOrderIdsByCustomerId(customerId, pageable);

            if (orderIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }

            List<Order> orders = orderRepository.findOrdersByIdsWithCustomers(orderIds.getContent());
            List<OrderSimpleDTO> dtos = orderMapper.ordersToOrderSimpleDTOs(orders);
            return new PageImpl<>(dtos, pageable, orderIds.getTotalElements());
        }

        // General order list: return with customer details (customer context NOT in URL)
        orderIds = orderRepository.findOrderIds(pageable);

        if (orderIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<Order> orders = orderRepository.findOrdersByIdsWithCustomers(orderIds.getContent());
        List<OrderDTO> dtos = orderMapper.ordersToOrderDTOs(orders);
        return new PageImpl<>(dtos, pageable, orderIds.getTotalElements());
    }

    /**
     * Get a specific order by ID with its customer details.
     *
     * @param id order ID
     * @return order with customer details, or 404 if not found
     */
    @GetMapping("/{id}")
    public OrderDTO getOrderById(@PathVariable Long id) {
        Order order = orderRepository
                .findByIdWithCustomer(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        return orderMapper.orderToOrderDTO(order);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDTO createOrder(@RequestBody Order order) {
        return orderMapper.orderToOrderDTO(orderRepository.save(order));
    }
}
