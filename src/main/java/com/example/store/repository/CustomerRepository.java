package com.example.store.repository;

import com.example.store.entity.Customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    /**
     * Get paginated customer IDs.
     */
    @Query("SELECT c.id FROM Customer c ORDER BY c.id")
    Page<Long> findCustomerIds(Pageable pageable);
    
    /**
     * Load customers.
     */
    @Query("SELECT c FROM Customer c WHERE c.id IN :ids ORDER BY c.id")
    List<Customer> findCustomersById(@Param("ids") List<Long> ids);
    
    /**
     * Get count of orders for each customer.
     */
    @Query("SELECT c.id, COUNT(o) FROM Customer c " +
           "LEFT JOIN c.orders o " +
           "WHERE c.id IN :ids " +
           "GROUP BY c.id")
    List<CustomerOrderCount> findOrderCountByCustomerIds(@Param("ids") List<Long> ids);
}