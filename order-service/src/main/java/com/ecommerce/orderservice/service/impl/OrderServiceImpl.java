package com.ecommerce.orderservice.service.impl;

import com.ecommerce.orderservice.dto.order.OrderDto;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.exception.wrapper.OrderNotFoundException;
import com.ecommerce.orderservice.helper.OrderMappingHelper;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.security.JwtTokenFilter;
import com.ecommerce.orderservice.service.CallAPI;
import com.ecommerce.orderservice.service.OrderService;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private final OrderRepository orderRepository;
    private final ModelMapper modelMapper;
    private final CallAPI callAPI;

    @Override
    public List<OrderDto> findAll() {
        log.info("OrderDto List, service; fetch all orders");
        Long currentUserId = currentUserId();
        if (isAdmin()) {
            return mapOrders(orderRepository.findAll());
        }
        return mapOrders(orderRepository.findAllByCart_UserId(currentUserId));
    }

    @Override
    public Page<OrderDto> findAll(int page, int size, String sortBy, String sortOrder) {
        log.info("OrderDto List, service; fetch all orders with paging and sorting");
        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Long currentUserId = currentUserId();
        Page<Order> orders = isAdmin()
                ? orderRepository.findAll(pageable)
                : orderRepository.findAllByCart_UserId(currentUserId, pageable);

        List<OrderDto> orderDtos = orders.stream()
                .map(OrderMappingHelper::map)
                .peek(this::attachProduct)
                .toList();
        return new PageImpl<>(orderDtos, pageable, orders.getTotalElements());
    }

    @Override
    public OrderDto findById(Integer orderId) {
        log.info("OrderDto, service; fetch order by id");
        Order order = loadOrderForCurrentUser(orderId);
        OrderDto orderDto = OrderMappingHelper.map(order);
        attachProduct(orderDto);
        return orderDto;
    }

    @Override
    public Boolean existsByOrderId(Integer orderId) {
        return isAdmin()
                ? orderRepository.findById(orderId).isPresent()
                : orderRepository.findByOrderIdAndCart_UserId(orderId, currentUserId()).isPresent();
    }

    @Override
    public OrderDto save(OrderDto orderDto) {
        log.info("OrderDto, service; save order");
        verifyCartOwnership(orderDto);
        return OrderMappingHelper.map(orderRepository.save(OrderMappingHelper.map(orderDto)));
    }

    @Override
    public OrderDto update(OrderDto orderDto) {
        log.info("OrderDto, service; update order");
        verifyCartOwnership(orderDto);
        return OrderMappingHelper.map(orderRepository.save(OrderMappingHelper.map(orderDto)));
    }

    @Override
    public OrderDto update(Integer orderId, OrderDto orderDto) {
        log.info("OrderDto, service; update order with orderId");
        Order existingOrder = loadOrderForCurrentUser(orderId);
        OrderDto existingOrderDto = OrderMappingHelper.map(existingOrder);
        modelMapper.map(orderDto, existingOrderDto);
        verifyCartOwnership(existingOrderDto);
        return OrderMappingHelper.map(orderRepository.save(OrderMappingHelper.map(existingOrderDto)));
    }

    @Override
    public void deleteById(Integer orderId) {
        log.info("Void, service; delete order by id");
        loadOrderForCurrentUser(orderId);
        orderRepository.deleteById(orderId);
    }

    private List<OrderDto> mapOrders(List<Order> orders) {
        return orders.stream()
                .map(order -> OrderMappingHelper.map(order))
                .peek(this::attachProduct)
                .toList();
    }

    private void attachProduct(OrderDto orderDto) {
        try {
            orderDto.setProductDto(callAPI.receiverProductDto(orderDto.getProductId()));
        } catch (Exception e) {
            log.error("Error fetching product info: {}", e.getMessage());
        }
    }

    private Order loadOrderForCurrentUser(Integer orderId) {
        if (isAdmin()) {
            return orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(String.format("Order with id: %d not found", orderId)));
        }
        return orderRepository.findByOrderIdAndCart_UserId(orderId, currentUserId())
                .orElseThrow(() -> new AccessDeniedException("You can only access your own orders"));
    }

    private void verifyCartOwnership(OrderDto orderDto) {
        if (isAdmin()) {
            return;
        }
        Long currentUserId = currentUserId();
        Long requestedUserId = orderDto.getCartDto() == null ? null : orderDto.getCartDto().getUserId();
        if (!Objects.equals(currentUserId, requestedUserId)) {
            throw new AccessDeniedException("You can only manage your own orders");
        }
    }

    private Long currentUserId() {
        return callAPI.receiverCurrentUserDto(JwtTokenFilter.getTokenFromRequest()).getId();
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
