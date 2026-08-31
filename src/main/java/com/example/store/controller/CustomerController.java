package com.example.store.controller;

import com.example.store.dto.CustomerDTO;
import com.example.store.dto.CustomerSummaryDTO;
import com.example.store.dto.OrderSimpleDTO;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.mapper.CustomerMapper;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.CustomerOrderCount;
import com.example.store.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 500;

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final CustomerMapper customerMapper;
    private final OrderMapper orderMapper;

    /**
     * Get all customers paginated.
     * @param page zero-based page number
     * @param size page size (default 50, max 500)
     * @return paginated list of customer summaries
     */
    @GetMapping
    public Page<CustomerSummaryDTO> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE + "") int size) {
        
        size = Math.min(size, MAX_PAGE_SIZE);
        size = Math.max(size, 1);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Long> customerIds = customerRepository.findCustomerIds(pageable);
        
        if (customerIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        
        // Load customers without orders (lightweight)
        List<Customer> customers = customerRepository.findCustomersById(
            customerIds.getContent());
        
        // Build order count map for efficient lookup
        List<CustomerOrderCount> counts = customerRepository.findOrderCountByCustomerIds(
            customerIds.getContent());
        Map<Long, Long> orderCountMap = new HashMap<>();
        for (CustomerOrderCount row : counts) {
            orderCountMap.put(row.customerId(), row.orderCount());
        }
        
        // Convert to summary DTOs with order counts
        List<CustomerSummaryDTO> summaries = customers.stream()
            .map(c -> {
                CustomerSummaryDTO dto = new CustomerSummaryDTO();
                dto.setId(c.getId());
                dto.setName(c.getName());
                dto.setOrderCount(orderCountMap.getOrDefault(c.getId(), 0L).intValue());
                return dto;
            })
            .toList();
        
        return new PageImpl<>(summaries, pageable, customerIds.getTotalElements());
    }

    /**
     * Get a specific customer by ID.
     * Returns customer details ONLY (no orders) to prevent unbounded relationship loading.
     * If you need this customer's orders, use GET /customer/{id}/orders instead.
     * 
     * @param id customer ID
     * @return customer summary with order count
     */
    @GetMapping("/{id}")
    public CustomerSummaryDTO getCustomerById(@PathVariable Long id) {
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
        
        // Get order count for this customer
        List<CustomerOrderCount> counts = customerRepository.findOrderCountByCustomerIds(List.of(id));
        int orderCount = counts.isEmpty() ? 0 : counts.get(0).orderCount().intValue();
        
        CustomerSummaryDTO dto = new CustomerSummaryDTO();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setOrderCount(orderCount);
        return dto;
    }

    /**
     * Get paginated orders for a specific customer.
     * 
     * @param customerId the customer ID
     * @param page zero-based page number
     * @param size page size (default 50, max 500)
     * @return paginated orders without customer details
     */
    @GetMapping("/{customerId}/orders")
    public Page<OrderSimpleDTO> getCustomerOrders(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE + "") int size) {
        
        size = Math.min(size, MAX_PAGE_SIZE);
        size = Math.max(size, 1);
        
        // Verify customer exists
        if (!customerRepository.existsById(customerId)) {
            throw new RuntimeException("Customer not found: " + customerId);
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Long> orderIds = orderRepository.findOrderIdsByCustomerId(customerId, pageable);
        
        if (orderIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        
        // Fetch orders WITHOUT customer data (customer context already known from URL)
        List<Order> orders = orderRepository.findOrdersByIdsWithCustomers(
            orderIds.getContent());
        
        // Map to simple DTO (id, description only - no customer)
        List<OrderSimpleDTO> dtos = orderMapper.ordersToOrderSimpleDTOs(orders);
        
        return new PageImpl<>(dtos, pageable, orderIds.getTotalElements());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDTO createCustomer(@RequestBody Customer customer) {
        return customerMapper.customerToCustomerDTO(customerRepository.save(customer));
    }
}
