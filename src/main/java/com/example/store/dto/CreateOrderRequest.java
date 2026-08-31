package com.example.store.dto;

import lombok.Data;

@Data
public class CreateOrderRequest {
    private String description;
    private Long customerId;
}
