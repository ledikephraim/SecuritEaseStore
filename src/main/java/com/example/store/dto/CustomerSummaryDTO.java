package com.example.store.dto;

import lombok.Data;

@Data
public class CustomerSummaryDTO {
    private Long id;
    private String name;
    private int orderCount;
}
