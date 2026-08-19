package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.Cart;
import com.ecommerce.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    @Modifying
    @Query("DELETE FROM Order o WHERE o.cart = :cart")
    void deleteAllByCart(Cart cart);

    List<Order> findAllByCart_UserId(Long userId);

    Page<Order> findAllByCart_UserId(Long userId, Pageable pageable);

    Optional<Order> findByOrderIdAndCart_UserId(Integer orderId, Long userId);
}
