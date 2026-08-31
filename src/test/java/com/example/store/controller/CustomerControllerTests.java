package com.example.store.controller;

import com.example.store.dto.CustomerDTO;
import com.example.store.dto.CustomerSummaryDTO;
import com.example.store.entity.Customer;
import com.example.store.exception.NotFoundException;
import com.example.store.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
class CustomerControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    private Customer customer;
    private CustomerSummaryDTO summary;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setName("John Smith");
        customer.setId(1L);

        summary = new CustomerSummaryDTO();
        summary.setId(1L);
        summary.setName("John Smith");
        summary.setOrderCount(0);
    }

    @Test
    void testCreateCustomer() throws Exception {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(1L);
        dto.setName("John Smith");
        when(customerService.createCustomer(any(Customer.class))).thenReturn(dto);

        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Smith"));
    }

    @Test
    void testGetAllCustomers() throws Exception {
        Page<CustomerSummaryDTO> page = new PageImpl<>(List.of(summary), PageRequest.of(0, 50), 1);
        when(customerService.getAllCustomers(eq(0), eq(50), isNull())).thenReturn(page);

        mockMvc.perform(get("/customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("John Smith"));
    }

    @Test
    void testGetAllCustomersPassesSearchQueryToService() throws Exception {
        Page<CustomerSummaryDTO> page = new PageImpl<>(List.of(summary), PageRequest.of(0, 50), 1);
        when(customerService.getAllCustomers(0, 50, "mit")).thenReturn(page);

        mockMvc.perform(get("/customer").param("q", "mit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("John Smith"));
    }

    @Test
    void testGetCustomerById() throws Exception {
        when(customerService.getCustomerById(1L)).thenReturn(summary);

        mockMvc.perform(get("/customer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Smith"));
    }

    @Test
    void testGetCustomerByIdReturns404WhenNotFound() throws Exception {
        when(customerService.getCustomerById(999L)).thenThrow(new NotFoundException("Customer not found: 999"));

        mockMvc.perform(get("/customer/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Customer not found: 999"));
    }
}
