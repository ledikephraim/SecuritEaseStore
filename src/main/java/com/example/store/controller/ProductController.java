package com.example.store.controller;

import com.example.store.dto.CreateProductRequest;
import com.example.store.dto.ProductResponse;
import com.example.store.service.ProductService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Get all products paginated, each with the list of order IDs it appears in.
     *
     * @param page zero-based page number
     * @param size page size (default 50, max 500)
     * @return paginated list of products
     */
    @GetMapping
    public Page<ProductResponse> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = ProductService.DEFAULT_PAGE_SIZE + "") int size) {
        return productService.getAllProducts(page, size);
    }

    /**
     * Get a specific product by ID, with the list of order IDs it appears in.
     *
     * @param id product ID
     * @return product details, or 404 if not found
     */
    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@RequestBody CreateProductRequest request) {
        return productService.createProduct(request);
    }
}
