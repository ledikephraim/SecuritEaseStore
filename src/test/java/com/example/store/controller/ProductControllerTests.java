package com.example.store.controller;

import com.example.store.dto.CreateProductRequest;
import com.example.store.dto.ProductResponse;
import com.example.store.exception.NotFoundException;
import com.example.store.service.ProductService;
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

@WebMvcTest(ProductController.class)
class ProductControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    private ProductResponse product;

    @BeforeEach
    void setUp() {
        product = new ProductResponse();
        product.setId(1L);
        product.setDescription("Widget");
        product.setOrderIds(List.of(10L, 20L));
    }

    @Test
    void testCreateProduct() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setDescription("Widget");

        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(product);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Widget"))
                .andExpect(jsonPath("$.orderIds").isArray());
    }

    @Test
    void testGetAllProducts() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(product), PageRequest.of(0, 50), 1);
        when(productService.getAllProducts(0, 50)).thenReturn(page);

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("Widget"))
                .andExpect(jsonPath("$.content[0].orderIds[0]").value(10))
                .andExpect(jsonPath("$.content[0].orderIds[1]").value(20));
    }

    @Test
    void testGetProductById() throws Exception {
        when(productService.getProductById(1L)).thenReturn(product);

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Widget"))
                .andExpect(jsonPath("$.orderIds[0]").value(10));
    }

    @Test
    void testGetProductByIdReturns404WhenNotFound() throws Exception {
        when(productService.getProductById(999L)).thenThrow(new NotFoundException("Product not found: 999"));

        mockMvc.perform(get("/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product not found: 999"));
    }
}
