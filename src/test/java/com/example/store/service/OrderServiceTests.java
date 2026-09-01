package com.example.store.service;

import com.example.store.dto.CreateOrderRequest;
import com.example.store.dto.OrderResponse;
import com.example.store.dto.OrderSimpleResponse;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.entity.Product;
import com.example.store.exception.NotFoundException;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTests {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private Customer customer;
    private Product product;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setName("John Smith");

        product = new Product();
        product.setId(5L);
        product.setDescription("Widget");
    }

    @Test
    void getAllOrders_withoutCustomerId_returnsFullOrderResponses() {
        Pageable pageable = PageRequest.of(0, 50);
        Order order = new Order();
        order.setId(10L);
        List<OrderResponse> mapped = List.of(new OrderResponse());

        when(orderRepository.findOrderIds(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(10L), pageable, 1));
        when(orderRepository.findOrdersByIdsWithCustomers(List.of(10L))).thenReturn(List.of(order));
        when(orderMapper.ordersToOrderResponses(List.of(order))).thenReturn(mapped);

        Page<?> result = (Page<?>) orderService.getAllOrders(0, 50, null);

        assertThat(result.getContent()).isEqualTo(mapped);
        verify(orderRepository, never()).findOrderIdsByCustomerId(any(), any());
    }

    @Test
    void getAllOrders_withCustomerId_returnsSimpleOrderResponses() {
        Pageable pageable = PageRequest.of(0, 50);
        Order order = new Order();
        order.setId(10L);
        List<OrderSimpleResponse> mapped = List.of(new OrderSimpleResponse());

        when(orderRepository.findOrderIdsByCustomerId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(10L), pageable, 1));
        when(orderRepository.findOrdersByIdsWithCustomers(List.of(10L))).thenReturn(List.of(order));
        when(orderMapper.ordersToOrderSimpleResponses(List.of(order))).thenReturn(mapped);

        Page<?> result = (Page<?>) orderService.getAllOrders(0, 50, 1L);

        assertThat(result.getContent()).isEqualTo(mapped);
        verify(orderRepository, never()).findOrderIds(any());
    }

    @Test
    void getAllOrders_returnsEmptyPage_whenNoOrdersMatch() {
        Pageable pageable = PageRequest.of(0, 50);
        when(orderRepository.findOrderIds(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<?> result = (Page<?>) orderService.getAllOrders(0, 50, null);

        assertThat(result.getContent()).isEmpty();
        verify(orderRepository, never()).findOrdersByIdsWithCustomers(anyList());
    }

    @Test
    void getOrderById_found_returnsMappedResponse() {
        Order order = new Order();
        order.setId(10L);
        OrderResponse response = new OrderResponse();

        when(orderRepository.findByIdWithCustomer(10L)).thenReturn(Optional.of(order));
        when(orderMapper.orderToOrderResponse(order)).thenReturn(response);

        assertThat(orderService.getOrderById(10L)).isEqualTo(response);
    }

    @Test
    void getOrderById_notFound_throwsNotFoundException() {
        when(orderRepository.findByIdWithCustomer(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void createOrder_customerNotFound_throwsNotFoundException() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(999L);
        request.setProductIds(List.of(5L));

        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_nullProductIds_throwsIllegalArgumentException() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setProductIds(null);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one product");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_emptyProductIds_throwsIllegalArgumentException() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setProductIds(List.of());

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one product");
    }

    @Test
    void createOrder_missingProduct_throwsNotFoundExceptionListingMissingIds() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setProductIds(List.of(5L, 999L));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findAllById(List.of(5L, 999L))).thenReturn(List.of(product));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_success_savesOrderWithCustomerAndProducts() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setDescription("New Order");
        request.setCustomerId(1L);
        request.setProductIds(List.of(5L));

        Order saved = new Order();
        saved.setId(20L);
        OrderResponse response = new OrderResponse();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findAllById(List.of(5L))).thenReturn(List.of(product));
        when(orderRepository.save(any(Order.class))).thenReturn(saved);
        when(orderMapper.orderToOrderResponse(saved)).thenReturn(response);

        OrderResponse result = orderService.createOrder(request);

        assertThat(result).isEqualTo(response);
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order captured = captor.getValue();
        assertThat(captured.getDescription()).isEqualTo("New Order");
        assertThat(captured.getCustomer()).isEqualTo(customer);
        assertThat(captured.getProducts()).containsExactly(product);
    }
}
