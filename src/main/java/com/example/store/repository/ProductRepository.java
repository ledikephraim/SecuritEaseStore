package com.example.store.repository;

import com.example.store.entity.Product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Get paginated product IDs. */
    @Query("SELECT p.id FROM Product p ORDER BY p.id")
    Page<Long> findProductIds(Pageable pageable);

    /** Load products. */
    @Query("SELECT p FROM Product p WHERE p.id IN :ids ORDER BY p.id")
    List<Product> findProductsById(@Param("ids") List<Long> ids);

    /** Get the IDs of the orders each product appears in. */
    @Query("SELECT new com.example.store.repository.ProductOrderRow(pr.id, o.id) "
            + "FROM Order o JOIN o.products pr "
            + "WHERE pr.id IN :productIds "
            + "ORDER BY pr.id, o.id")
    List<ProductOrderRow> findOrderIdsByProductIds(@Param("productIds") List<Long> productIds);
}
