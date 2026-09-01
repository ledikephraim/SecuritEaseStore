package com.example.store.service;

import com.example.store.dto.CreateOrderRequest;
import com.example.store.dto.OrderResponse;
import com.example.store.dto.OrderSimpleResponse;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.entity.Product;
import com.example.store.exception.NotFoundException;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 500;

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
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
            List<OrderSimpleResponse> dtos = orderMapper.ordersToOrderSimpleResponses(orders);
            return new PageImpl<>(dtos, pageable, orderIds.getTotalElements());
        }

        orderIds = orderRepository.findOrderIds(pageable);

        if (orderIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<Order> orders = orderRepository.findOrdersByIdsWithCustomers(orderIds.getContent());
        List<OrderResponse> dtos = orderMapper.ordersToOrderResponses(orders);
        return new PageImpl<>(dtos, pageable, orderIds.getTotalElements());
    }

    /** Get a specific order by ID with its customer details. */
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository
                .findByIdWithCustomer(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
        return orderMapper.orderToOrderResponse(order);
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Customer customer = customerRepository
                .findById(request.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found: " + request.getCustomerId()));

        List<Long> productIds = request.getProductIds();
        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("An order must contain at least one product");
        }

        List<Product> products = productRepository.findAllById(productIds);
        if (products.size() != new HashSet<>(productIds).size()) {
            Set<Long> foundIds = new HashSet<>();
            for (Product product : products) {
                foundIds.add(product.getId());
            }
            List<Long> missingIds = productIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .distinct()
                    .toList();
            throw new NotFoundException("Product(s) not found: " + missingIds);
        }

        Order order = new Order();
        order.setDescription(request.getDescription());
        order.setCustomer(customer);
        order.setProducts(products);

        Order saved = orderRepository.save(order);
        log.info("Created order {} for customer {} with products {}", saved.getId(), customer.getId(), productIds);
        return orderMapper.orderToOrderResponse(saved);
    }

    private static int clampPageSize(int size) {
        return Math.max(Math.min(size, MAX_PAGE_SIZE), 1);
    }
}
