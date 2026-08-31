package com.example.store.mapper;

import com.example.store.dto.OrderCustomerResponse;
import com.example.store.dto.OrderResponse;
import com.example.store.dto.OrderSimpleResponse;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;

import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    OrderResponse orderToOrderResponse(Order order);

    List<OrderResponse> ordersToOrderResponses(List<Order> orders);

    OrderSimpleResponse orderToOrderSimpleResponse(Order order);

    List<OrderSimpleResponse> ordersToOrderSimpleResponses(List<Order> orders);

    OrderCustomerResponse orderToOrderCustomerResponse(Customer customer);
}
