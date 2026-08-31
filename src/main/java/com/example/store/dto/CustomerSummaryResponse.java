package com.example.store.dto;

import lombok.Data;

@Data
public class CustomerSummaryResponse {
    private Long id;
    private String name;
    private int orderCount;
}
