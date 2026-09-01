package com.example.store.service;

import com.example.store.dto.CreateCustomerRequest;
import com.example.store.dto.CustomerResponse;
import com.example.store.dto.CustomerSummaryResponse;
import com.example.store.dto.OrderSimpleResponse;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.exception.NotFoundException;
import com.example.store.mapper.CustomerMapper;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.CustomerOrderCount;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;

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
class CustomerServiceTests {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setName("John Smith");
    }

    @Test
    void getAllCustomers_withoutQuery_usesUnfilteredLookup() {
        Pageable pageable = PageRequest.of(0, 50);
        when(customerRepository.findCustomerIds(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(1L), pageable, 1));
        when(customerRepository.findCustomersById(List.of(1L))).thenReturn(List.of(customer));
        when(customerRepository.findOrderCountByCustomerIds(List.of(1L)))
                .thenReturn(List.of(new CustomerOrderCount(1L, 3L)));

        Page<CustomerSummaryResponse> result = customerService.getAllCustomers(0, 50, null);

        assertThat(result.getContent()).hasSize(1);
        CustomerSummaryResponse summary = result.getContent().get(0);
        assertThat(summary.getId()).isEqualTo(1L);
        assertThat(summary.getName()).isEqualTo("John Smith");
        assertThat(summary.getOrderCount()).isEqualTo(3);
        verify(customerRepository, never()).findCustomerIdsByNameContaining(any(), any());
    }

    @Test
    void getAllCustomers_withBlankQuery_treatedAsUnfiltered() {
        Pageable pageable = PageRequest.of(0, 50);
        when(customerRepository.findCustomerIds(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<CustomerSummaryResponse> result = customerService.getAllCustomers(0, 50, "   ");

        assertThat(result.getContent()).isEmpty();
        verify(customerRepository, never()).findCustomerIdsByNameContaining(any(), any());
    }

    @Test
    void getAllCustomers_withQuery_usesNameSearch() {
        Pageable pageable = PageRequest.of(0, 50);
        when(customerRepository.findCustomerIdsByNameContaining(eq("smith"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(1L), pageable, 1));
        when(customerRepository.findCustomersById(List.of(1L))).thenReturn(List.of(customer));
        when(customerRepository.findOrderCountByCustomerIds(List.of(1L))).thenReturn(List.of());

        Page<CustomerSummaryResponse> result = customerService.getAllCustomers(0, 50, "  smith  ");

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOrderCount()).isZero();
        verify(customerRepository).findCustomerIdsByNameContaining(eq("smith"), any(Pageable.class));
    }

    @Test
    void getAllCustomers_returnsEmptyPage_withoutFurtherLookups_whenNoIdsMatch() {
        Pageable pageable = PageRequest.of(0, 50);
        when(customerRepository.findCustomerIds(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<CustomerSummaryResponse> result = customerService.getAllCustomers(0, 50, null);

        assertThat(result.getContent()).isEmpty();
        verify(customerRepository, never()).findCustomersById(anyList());
        verify(customerRepository, never()).findOrderCountByCustomerIds(anyList());
    }

    @Test
    void getAllCustomers_clampsPageSizeAboveMax() {
        when(customerRepository.findCustomerIds(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, CustomerService.MAX_PAGE_SIZE), 0));

        customerService.getAllCustomers(0, 10_000, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(customerRepository).findCustomerIds(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(CustomerService.MAX_PAGE_SIZE);
    }

    @Test
    void getAllCustomers_clampsPageSizeBelowOne() {
        when(customerRepository.findCustomerIds(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));

        customerService.getAllCustomers(0, 0, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(customerRepository).findCustomerIds(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void getCustomerById_found_returnsSummaryWithOrderCount() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.findOrderCountByCustomerIds(List.of(1L)))
                .thenReturn(List.of(new CustomerOrderCount(1L, 5L)));

        CustomerSummaryResponse result = customerService.getCustomerById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOrderCount()).isEqualTo(5);
    }

    @Test
    void getCustomerById_notFound_throwsNotFoundException() {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void getCustomerOrders_customerNotFound_throwsNotFoundException() {
        when(customerRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> customerService.getCustomerOrders(999L, 0, 50))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");
        verify(orderRepository, never()).findOrderIdsByCustomerId(any(), any());
    }

    @Test
    void getCustomerOrders_returnsMappedOrders() {
        Pageable pageable = PageRequest.of(0, 50);
        Order order = new Order();
        order.setId(10L);
        List<OrderSimpleResponse> mapped = List.of(new OrderSimpleResponse());

        when(customerRepository.existsById(1L)).thenReturn(true);
        when(orderRepository.findOrderIdsByCustomerId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(10L), pageable, 1));
        when(orderRepository.findOrdersByIdsWithCustomers(List.of(10L))).thenReturn(List.of(order));
        when(orderMapper.ordersToOrderSimpleResponses(List.of(order))).thenReturn(mapped);

        Page<OrderSimpleResponse> result = customerService.getCustomerOrders(1L, 0, 50);

        assertThat(result.getContent()).isEqualTo(mapped);
    }

    @Test
    void createCustomer_savesAndReturnsMappedResponse() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setName("New Customer");

        Customer saved = new Customer();
        saved.setId(2L);
        saved.setName("New Customer");

        CustomerResponse response = new CustomerResponse();
        response.setId(2L);
        response.setName("New Customer");

        when(customerRepository.save(any(Customer.class))).thenReturn(saved);
        when(customerMapper.customerToCustomerResponse(saved)).thenReturn(response);

        CustomerResponse result = customerService.createCustomer(request);

        assertThat(result).isEqualTo(response);
        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("New Customer");
    }
}
