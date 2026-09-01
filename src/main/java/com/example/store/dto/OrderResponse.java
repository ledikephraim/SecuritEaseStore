package com.example.store.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderResponse {
    private Long id;
    private String description;
    private OrderCustomerResponse customer;
    private List<OrderProductResponse> products;
}
