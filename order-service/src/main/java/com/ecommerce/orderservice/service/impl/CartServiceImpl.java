package com.ecommerce.orderservice.service.impl;


import com.ecommerce.orderservice.dto.order.CartDto;
import com.ecommerce.orderservice.entity.Cart;
import com.ecommerce.orderservice.exception.wrapper.CartNotFoundException;
import com.ecommerce.orderservice.helper.CartMappingHelper;
import com.ecommerce.orderservice.repository.CartRepository;
import com.ecommerce.orderservice.security.JwtTokenFilter;
import com.ecommerce.orderservice.service.CallAPI;
import com.ecommerce.orderservice.service.CartService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;


@RequiredArgsConstructor
@Service
public class CartServiceImpl implements CartService {
    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartRepository cartRepository;
    private final ModelMapper modelMapper;
    private final CallAPI callAPI;

    @Override
    public List<CartDto> findAll() {
        log.info("CartDto List, service; fetch all carts");
        Long currentUserId = currentUserId();
        if (isAdmin()) {
            return mapCarts(cartRepository.findAll());
        }
        return mapCarts(cartRepository.findAllByUserId(currentUserId));
    }
    @Override
    public Page<CartDto> findAll(int page, int size, String sortBy, String sortOrder) {
        log.info("CartDto List, service; fetch all carts with paging and sorting");
        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Long currentUserId = currentUserId();
        Page<Cart> carts = isAdmin()
                ? cartRepository.findAll(pageable)
                : cartRepository.findAllByUserId(currentUserId, pageable);

        List<CartDto> cartDtos = carts.stream()
                .map(CartMappingHelper::map)
                .peek(this::attachUser)
                .toList();
        return new PageImpl<>(cartDtos, pageable, carts.getTotalElements());
    }
    @Override
    public CartDto findById(Integer cartId) {
        log.info("CartDto, service; fetch cart by id");
        Cart cart = loadCartForCurrentUser(cartId);
        CartDto cartDto = CartMappingHelper.map(cart);
        attachUser(cartDto);
        return cartDto;
    }

    @Override
    public CartDto save(CartDto cartDto) {
        log.info("CartDto, service; save cart");
        if (cartDto == null) {
            throw new CartNotFoundException("Cart data must not be null");
        }
        if (!isAdmin()) {
            // The authenticated principal is the source of truth for ownership.
            cartDto.setUserId(currentUserId());
        }
        verifyCartOwnership(cartDto);
        Cart cart = CartMappingHelper.map(cartDto);
        Cart savedCart = cartRepository.save(cart);
        return CartMappingHelper.map(savedCart);
    }
    @Override
    public CartDto update(CartDto cartDto) {
        log.info("CartDto, service; update cart");
        verifyCartOwnership(cartDto);
        return CartMappingHelper.map(cartRepository.save(CartMappingHelper.map(cartDto)));
    }

    @Override
    public CartDto update(Integer cartId, CartDto cartDto) {
        log.info("CartDto, service; update cart with cartId");
        Cart existingCart = loadCartForCurrentUser(cartId);
        CartDto existingCartDto = CartMappingHelper.map(existingCart);
        modelMapper.map(cartDto, existingCartDto);
        verifyCartOwnership(existingCartDto);
        return CartMappingHelper.map(cartRepository.save(CartMappingHelper.map(existingCartDto)));
    }

    @Override
    public void deleteById(Integer cartId) {
        log.info("Void, service; delete cart by id");
        Cart cart = loadCartForCurrentUser(cartId);
        cartRepository.delete(cart);
    }

    private List<CartDto> mapCarts(List<Cart> carts) {
        return carts.stream()
                .map(CartMappingHelper::map)
                .peek(this::attachUser)
                .toList();
    }

    private void attachUser(CartDto cartDto) {
        try {
            cartDto.setUserDto(callAPI.receiverUserDto(cartDto.getUserId(), JwtTokenFilter.getTokenFromRequest()));
        } catch (Exception e) {
            log.error("Error fetching user info: {}", e.getMessage());
        }
    }

    private Cart loadCartForCurrentUser(Integer cartId) {
        if (isAdmin()) {
            return cartRepository.findById(cartId)
                    .orElseThrow(() -> new CartNotFoundException(String.format("Cart with id: %d not found", cartId)));
        }
        return cartRepository.findByCartIdAndUserId(cartId, currentUserId())
                .orElseThrow(() -> new AccessDeniedException("You can only access your own carts"));
    }

    private void verifyCartOwnership(CartDto cartDto) {
        if (isAdmin()) {
            return;
        }
        Long currentUserId = currentUserId();
        if (!Objects.equals(currentUserId, cartDto.getUserId())) {
            throw new AccessDeniedException("You can only manage your own carts");
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
