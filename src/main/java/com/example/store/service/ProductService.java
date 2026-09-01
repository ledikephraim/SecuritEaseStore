package com.example.store.service;

import com.example.store.dto.CreateProductRequest;
import com.example.store.dto.ProductResponse;
import com.example.store.entity.Product;
import com.example.store.exception.NotFoundException;
import com.example.store.repository.ProductOrderRow;
import com.example.store.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductService {

    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 500;

    private final ProductRepository productRepository;

    /** Get all products paginated, each with the list of order IDs it appears in. */
    public Page<ProductResponse> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, clampPageSize(size));
        Page<Long> productIds = productRepository.findProductIds(pageable);

        if (productIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<Product> products = productRepository.findProductsById(productIds.getContent());
        Map<Long, List<Long>> orderIdsByProduct =
                groupOrderIdsByProduct(productRepository.findOrderIdsByProductIds(productIds.getContent()));

        List<ProductResponse> responses = products.stream()
                .map(p -> toResponse(p, orderIdsByProduct.getOrDefault(p.getId(), List.of())))
                .toList();

        return new PageImpl<>(responses, pageable, productIds.getTotalElements());
    }

    /** Get a specific product by ID, with the list of order IDs it appears in. */
    public ProductResponse getProductById(Long id) {
        Product product =
                productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found: " + id));

        List<Long> orderIds = groupOrderIdsByProduct(productRepository.findOrderIdsByProductIds(List.of(id)))
                .getOrDefault(id, List.of());

        return toResponse(product, orderIds);
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Product product = new Product();
        product.setDescription(request.getDescription());
        return toResponse(productRepository.save(product), List.of());
    }

    private static Map<Long, List<Long>> groupOrderIdsByProduct(List<ProductOrderRow> rows) {
        Map<Long, List<Long>> orderIdsByProduct = new HashMap<>();
        for (ProductOrderRow row : rows) {
            orderIdsByProduct
                    .computeIfAbsent(row.productId(), id -> new ArrayList<>())
                    .add(row.orderId());
        }
        return orderIdsByProduct;
    }

    private static int clampPageSize(int size) {
        return Math.max(Math.min(size, MAX_PAGE_SIZE), 1);
    }

    private static ProductResponse toResponse(Product product, List<Long> orderIds) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setDescription(product.getDescription());
        response.setOrderIds(orderIds);
        return response;
    }
}
