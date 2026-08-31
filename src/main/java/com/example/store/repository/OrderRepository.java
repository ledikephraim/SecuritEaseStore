package com.example.store.repository;

import com.example.store.entity.Order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Fetch all orders with their associated customers in a single query. */
    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.customer")
    List<Order> findAllWithCustomers();

    /** Fetch a specific order with its associated customer in a single query. */
    @Query("SELECT o FROM Order o JOIN FETCH o.customer WHERE o.id = :id")
    Optional<Order> findByIdWithCustomer(Long id);

    /** Get paginated order IDs. */
    @Query("SELECT o.id FROM Order o ORDER BY o.id")
    Page<Long> findOrderIds(Pageable pageable);

    /** Fetch orders with their customers by IDs. */
    @Query("SELECT DISTINCT o FROM Order o " + "JOIN FETCH o.customer " + "WHERE o.id IN :ids " + "ORDER BY o.id")
    List<Order> findOrdersByIdsWithCustomers(@Param("ids") List<Long> ids);

    /** Returns paginated order IDs for a customer. */
    @Query("SELECT o.id FROM Order o WHERE o.customer.id = :customerId ORDER BY o.id")
    Page<Long> findOrderIdsByCustomerId(@Param("customerId") Long customerId, Pageable pageable);
}
