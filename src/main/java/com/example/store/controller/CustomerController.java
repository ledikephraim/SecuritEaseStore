package com.example.store.controller;

import com.example.store.dto.CustomerDTO;
import com.example.store.dto.CustomerSummaryDTO;
import com.example.store.dto.OrderSimpleDTO;
import com.example.store.entity.Customer;
import com.example.store.service.CustomerService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Get all customers paginated, optionally filtered by a substring match against one of the words in the customer's
     * name (case-insensitive).
     *
     * @param page zero-based page number
     * @param size page size (default 50, max 500)
     * @param q optional substring to match against a word in the customer's name
     * @return paginated list of customer summaries
     */
    @GetMapping
    public Page<CustomerSummaryDTO> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = CustomerService.DEFAULT_PAGE_SIZE + "") int size,
            @RequestParam(required = false) String q) {
        return customerService.getAllCustomers(page, size, q);
    }

    /**
     * Get a specific customer by ID. Returns customer details ONLY (no orders) to prevent unbounded relationship
     * loading. If you need this customer's orders, use GET /customer/{id}/orders instead.
     *
     * @param id customer ID
     * @return customer summary with order count
     */
    @GetMapping("/{id}")
    public CustomerSummaryDTO getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id);
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
            @RequestParam(defaultValue = CustomerService.DEFAULT_PAGE_SIZE + "") int size) {
        return customerService.getCustomerOrders(customerId, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDTO createCustomer(@RequestBody Customer customer) {
        return customerService.createCustomer(customer);
    }
}
