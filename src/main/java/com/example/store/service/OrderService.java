package com.example.store.service;

import com.example.store.dto.OrderDTO;
import com.example.store.dto.OrderSimpleDTO;
import com.example.store.entity.Order;
import com.example.store.exception.NotFoundException;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 500;

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    /**
     * Get all orders with pagination, optionally filtered by customer ID.
     *
     * <p>When filtered by customer, orders are returned WITHOUT customer details (already known from the filter);
     * otherwise customer details are included.
     */
    public Object getAllOrders(int page, int size, Long customerId) {
        Pageable pageable = PageRequest.of(page, clampPageSize(size));
        Page<Long> orderIds;

        if (customerId != null) {
            orderIds = orderRepository.findOrderIdsByCustomerId(customerId, pageable);

            if (orderIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }

            List<Order> orders = orderRepository.findOrdersByIdsWithCustomers(orderIds.getContent());
            List<OrderSimpleDTO> dtos = orderMapper.ordersToOrderSimpleDTOs(orders);
            return new PageImpl<>(dtos, pageable, orderIds.getTotalElements());
        }

        orderIds = orderRepository.findOrderIds(pageable);

        if (orderIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<Order> orders = orderRepository.findOrdersByIdsWithCustomers(orderIds.getContent());
        List<OrderDTO> dtos = orderMapper.ordersToOrderDTOs(orders);
        return new PageImpl<>(dtos, pageable, orderIds.getTotalElements());
    }

    /** Get a specific order by ID with its customer details. */
    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository
                .findByIdWithCustomer(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
        return orderMapper.orderToOrderDTO(order);
    }

    @Transactional
    public OrderDTO createOrder(Order order) {
        return orderMapper.orderToOrderDTO(orderRepository.save(order));
    }

    private static int clampPageSize(int size) {
        return Math.max(Math.min(size, MAX_PAGE_SIZE), 1);
    }
}
