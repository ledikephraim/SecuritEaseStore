package com.example.store.service;

import com.example.store.dto.CustomerDTO;
import com.example.store.dto.CustomerSummaryDTO;
import com.example.store.dto.OrderSimpleDTO;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.mapper.CustomerMapper;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.CustomerOrderCount;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

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
    public Page<CustomerSummaryDTO> getAllCustomers(int page, int size, String q) {
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

        List<CustomerSummaryDTO> summaries = customers.stream()
                .map(c -> toSummaryDTO(c, orderCountMap.getOrDefault(c.getId(), 0L)))
                .toList();

        return new PageImpl<>(summaries, pageable, customerIds.getTotalElements());
    }

    /**
     * Get a specific customer by ID. Returns customer details ONLY (no orders) to prevent unbounded relationship
     * loading. If you need this customer's orders, use {@link #getCustomerOrders(Long, int, int)} instead.
     */
    public CustomerSummaryDTO getCustomerById(Long id) {
        Customer customer =
                customerRepository.findById(id).orElseThrow(() -> new RuntimeException("Customer not found: " + id));

        List<CustomerOrderCount> counts = customerRepository.findOrderCountByCustomerIds(List.of(id));
        long orderCount = counts.isEmpty() ? 0 : counts.get(0).orderCount();

        return toSummaryDTO(customer, orderCount);
    }

    /** Get paginated orders for a specific customer. */
    public Page<OrderSimpleDTO> getCustomerOrders(Long customerId, int page, int size) {
        if (!customerRepository.existsById(customerId)) {
            throw new RuntimeException("Customer not found: " + customerId);
        }

        Pageable pageable = PageRequest.of(page, clampPageSize(size));
        Page<Long> orderIds = orderRepository.findOrderIdsByCustomerId(customerId, pageable);

        if (orderIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // Fetch orders WITHOUT customer data (customer context already known from URL)
        List<Order> orders = orderRepository.findOrdersByIdsWithCustomers(orderIds.getContent());
        List<OrderSimpleDTO> dtos = orderMapper.ordersToOrderSimpleDTOs(orders);

        return new PageImpl<>(dtos, pageable, orderIds.getTotalElements());
    }

    @Transactional
    public CustomerDTO createCustomer(Customer customer) {
        return customerMapper.customerToCustomerDTO(customerRepository.save(customer));
    }

    private static int clampPageSize(int size) {
        return Math.max(Math.min(size, MAX_PAGE_SIZE), 1);
    }

    private static CustomerSummaryDTO toSummaryDTO(Customer customer, long orderCount) {
        CustomerSummaryDTO dto = new CustomerSummaryDTO();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setOrderCount((int) orderCount);
        return dto;
    }
}
