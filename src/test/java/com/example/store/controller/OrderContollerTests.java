package com.example.store.controller;

import com.example.store.dto.CreateOrderRequest;
import com.example.store.dto.OrderCustomerResponse;
import com.example.store.dto.OrderResponse;
import com.example.store.exception.NotFoundException;
import com.example.store.service.OrderService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        OrderCustomerResponse customerResponse = new OrderCustomerResponse();
        customerResponse.setId(1L);
        customerResponse.setName("John Doe");

        orderResponse = new OrderResponse();
        orderResponse.setId(1L);
        orderResponse.setDescription("Test Order");
        orderResponse.setCustomer(customerResponse);
    }

    @Test
    void testCreateOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setDescription("Test Order");
        request.setCustomerId(1L);

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(orderResponse);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Test Order"))
                .andExpect(jsonPath("$.customer.name").value("John Doe"));
    }

    @Test
    void testGetAllOrders() throws Exception {
        Page<OrderResponse> page = new PageImpl<>(List.of(orderResponse), PageRequest.of(0, 50), 1);
        when(orderService.getAllOrders(0, 50, null)).thenReturn(page);

        mockMvc.perform(get("/order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..description").value("Test Order"))
                .andExpect(jsonPath("$..customer.name").value("John Doe"));
    }

    @Test
    void testGetOrderById() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(orderResponse);

        mockMvc.perform(get("/order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Test Order"));
    }

    @Test
    void testGetOrderByIdReturns404WhenNotFound() throws Exception {
        when(orderService.getOrderById(999L)).thenThrow(new NotFoundException("Order not found: 999"));

        mockMvc.perform(get("/order/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Order not found: 999"));
    }
}
