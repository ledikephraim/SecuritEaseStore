package com.example.store.mapper;

import com.example.store.dto.CustomerResponse;
import com.example.store.entity.Customer;

import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    CustomerResponse customerToCustomerResponse(Customer customer);

    List<CustomerResponse> customersToCustomerResponses(List<Customer> customer);
}
