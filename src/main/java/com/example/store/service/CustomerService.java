package com.example.store.service;

import com.example.store.dto.CreateCustomerRequest;
import com.example.store.dto.CustomerResponse;
import com.example.store.dto.CustomerSummaryResponse;
import com.example.store.dto.OrderSimpleResponse;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.exception.NotFoundException;
import com.example.store.mapper.CustomerMapper;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.CustomerOrderCount;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 500;

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final CustomerMapper customerMapper;
    private final OrderMapper orderMapper;

    /**
     * Get all customers paginated, optionally filtered by a substring match against one of the words in the customer's
     * name (case-insensitive).
     */
    public Page<CustomerSummaryResponse> getAllCustomers(int page, int size, String q) {
        Pageable pageable = PageRequest.of(page, clampPageSize(size));
        String query = q == null ? null : q.trim();

        Page<Long> customerIds = (query == null || query.isEmpty())
                ? customerRepository.findCustomerIds(pageable)
                : customerRepository.findCustomerIdsByNameContaining(query, pageable);

        if (customerIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // Load customers without orders
        List<Customer> customers = customerRepository.findCustomersById(customerIds.getContent());

        // Build order count map for efficient lookup
        List<CustomerOrderCount> counts = customerRepository.findOrderCountByCustomerIds(customerIds.getContent());
        Map<Long, Long> orderCountMap = new HashMap<>();
        for (CustomerOrderCount row : counts) {
            orderCountMap.put(row.customerId(), row.orderCount());
        }

        List<CustomerSummaryResponse> summaries = customers.stream()
                .map(c -> toSummaryResponse(c, orderCountMap.getOrDefault(c.getId(), 0L)))
                .toList();

        return new PageImpl<>(summaries, pageable, customerIds.getTotalElements());
    }

    /**
     * Get a specific customer by ID. Returns customer details ONLY (no orders) to prevent unbounded relationship
     * loading. If you need this customer's orders, use {@link #getCustomerOrders(Long, int, int)} instead.
     */
    public CustomerSummaryResponse getCustomerById(Long id) {
        Customer customer =
                customerRepository.findById(id).orElseThrow(() -> new NotFoundException("Customer not found: " + id));

        List<CustomerOrderCount> counts = customerRepository.findOrderCountByCustomerIds(List.of(id));
        long orderCount = counts.isEmpty() ? 0 : counts.get(0).orderCount();

        return toSummaryResponse(customer, orderCount);
    }

    /** Get paginated orders for a specific customer. */
    public Page<OrderSimpleResponse> getCustomerOrders(Long customerId, int page, int size) {
        if (!customerRepository.existsById(customerId)) {
            throw new NotFoundException("Customer not found: " + customerId);
        }

        Pageable pageable = PageRequest.of(page, clampPageSize(size));
        Page<Long> orderIds = orderRepository.findOrderIdsByCustomerId(customerId, pageable);

        if (orderIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // Fetch orders WITHOUT customer data (customer context already known from URL)
        List<Order> orders = orderRepository.findOrdersByIdsWithCustomers(orderIds.getContent());
        List<OrderSimpleResponse> dtos = orderMapper.ordersToOrderSimpleResponses(orders);

        return new PageImpl<>(dtos, pageable, orderIds.getTotalElements());
    }

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        Customer customer = new Customer();
        customer.setName(request.getName());
        Customer saved = customerRepository.save(customer);
        log.info("Created customer {}", saved.getId());
        return customerMapper.customerToCustomerResponse(saved);
    }

    private static int clampPageSize(int size) {
        return Math.max(Math.min(size, MAX_PAGE_SIZE), 1);
    }

    private static CustomerSummaryResponse toSummaryResponse(Customer customer, long orderCount) {
        CustomerSummaryResponse response = new CustomerSummaryResponse();
        response.setId(customer.getId());
        response.setName(customer.getName());
        response.setOrderCount((int) orderCount);
        return response;
    }
}
