package com.example.store.controller;

import com.example.store.dto.OrderDTO;
import com.example.store.entity.Order;
import com.example.store.service.OrderService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

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
            @RequestParam(defaultValue = OrderService.DEFAULT_PAGE_SIZE + "") int size,
            @RequestParam(required = false) Long customerId) {
        return orderService.getAllOrders(page, size, customerId);
    }

    /**
     * Get a specific order by ID with its customer details.
     *
     * @param id order ID
     * @return order with customer details, or 404 if not found
     */
    @GetMapping("/{id}")
    public OrderDTO getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDTO createOrder(@RequestBody Order order) {
        return orderService.createOrder(order);
    }
}
