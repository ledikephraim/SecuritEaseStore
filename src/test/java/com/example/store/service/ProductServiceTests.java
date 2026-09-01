package com.example.store.service;

import com.example.store.dto.CreateProductRequest;
import com.example.store.dto.ProductResponse;
import com.example.store.entity.Product;
import com.example.store.exception.NotFoundException;
import com.example.store.repository.ProductOrderRow;
import com.example.store.repository.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTests {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setDescription("Widget");
    }

    @Test
    void getAllProducts_returnsEmptyPage_withoutFurtherLookups_whenNoIdsMatch() {
        Pageable pageable = PageRequest.of(0, 50);
        when(productRepository.findProductIds(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<ProductResponse> result = productService.getAllProducts(0, 50);

        assertThat(result.getContent()).isEmpty();
        verify(productRepository, never()).findProductsById(anyList());
        verify(productRepository, never()).findOrderIdsByProductIds(anyList());
    }

    @Test
    void getAllProducts_groupsOrderIdsByProduct() {
        Pageable pageable = PageRequest.of(0, 50);
        Product other = new Product();
        other.setId(2L);
        other.setDescription("Gadget");

        when(productRepository.findProductIds(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(1L, 2L), pageable, 2));
        when(productRepository.findProductsById(List.of(1L, 2L))).thenReturn(List.of(product, other));
        when(productRepository.findOrderIdsByProductIds(List.of(1L, 2L)))
                .thenReturn(List.of(new ProductOrderRow(1L, 100L), new ProductOrderRow(1L, 200L)));

        Page<ProductResponse> result = productService.getAllProducts(0, 50);

        assertThat(result.getContent()).hasSize(2);
        ProductResponse first = result.getContent().get(0);
        assertThat(first.getId()).isEqualTo(1L);
        assertThat(first.getOrderIds()).containsExactly(100L, 200L);

        ProductResponse second = result.getContent().get(1);
        assertThat(second.getId()).isEqualTo(2L);
        assertThat(second.getOrderIds()).isEmpty();
    }

    @Test
    void getProductById_found_returnsResponseWithOrderIds() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.findOrderIdsByProductIds(List.of(1L)))
                .thenReturn(List.of(new ProductOrderRow(1L, 100L)));

        ProductResponse result = productService.getProductById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDescription()).isEqualTo("Widget");
        assertThat(result.getOrderIds()).containsExactly(100L);
    }

    @Test
    void getProductById_notFound_throwsNotFoundException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void createProduct_savesAndReturnsResponseWithEmptyOrderIds() {
        CreateProductRequest request = new CreateProductRequest();
        request.setDescription("New Product");

        Product saved = new Product();
        saved.setId(2L);
        saved.setDescription("New Product");

        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponse result = productService.createProduct(request);

        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getDescription()).isEqualTo("New Product");
        assertThat(result.getOrderIds()).isEmpty();
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("New Product");
    }
}
