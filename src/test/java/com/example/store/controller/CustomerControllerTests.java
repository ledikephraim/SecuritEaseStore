package com.example.store.controller;

import com.example.store.entity.Customer;
import com.example.store.mapper.CustomerMapper;
import com.example.store.repository.CustomerOrderCount;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@ComponentScan(basePackageClasses = CustomerMapper.class)
class CustomerControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private OrderRepository orderRepository;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setName("John Smith");
        customer.setId(1L);
    }

    @Test
    void testCreateCustomer() throws Exception {
        when(customerRepository.save(customer)).thenReturn(customer);

        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Smith"));
    }

    @Test
    void testGetAllCustomers() throws Exception {
        Pageable pageable = PageRequest.of(0, 50);
        when(customerRepository.findCustomerIds(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(1L), pageable, 1));
        when(customerRepository.findCustomersById(anyList())).thenReturn(List.of(customer));
        when(customerRepository.findOrderCountByCustomerIds(anyList()))
                .thenReturn(List.of(new CustomerOrderCount(1L, 0L)));

        mockMvc.perform(get("/customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("John Smith"));

        verify(customerRepository, never()).findCustomerIdsByNameContaining(any(), any());
    }

    @Test
    void testGetAllCustomersFiltersByNameSubstring() throws Exception {
        Pageable pageable = PageRequest.of(0, 50);
        when(customerRepository.findCustomerIdsByNameContaining(eq("mit"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(1L), pageable, 1));
        when(customerRepository.findCustomersById(anyList())).thenReturn(List.of(customer));
        when(customerRepository.findOrderCountByCustomerIds(anyList()))
                .thenReturn(List.of(new CustomerOrderCount(1L, 0L)));

        mockMvc.perform(get("/customer").param("q", "mit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("John Smith"));

        verify(customerRepository, never()).findCustomerIds(any(Pageable.class));
    }

    @Test
    void testGetAllCustomersTrimsAndIgnoresBlankQuery() throws Exception {
        Pageable pageable = PageRequest.of(0, 50);
        when(customerRepository.findCustomerIds(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        mockMvc.perform(get("/customer").param("q", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(customerRepository, never()).findCustomerIdsByNameContaining(any(), any());
    }

    @Test
    void testGetAllCustomersReturnsEmptyPageWhenNoMatch() throws Exception {
        Pageable pageable = PageRequest.of(0, 50);
        when(customerRepository.findCustomerIdsByNameContaining(eq("zzz"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        mockMvc.perform(get("/customer").param("q", "zzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(customerRepository, never()).findCustomersById(anyList());
    }
}
