package com.pavitraristaa.subscriptions.repository;

import com.pavitraristaa.subscriptions.entity.Coupon;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeIgnoreCaseAndActiveTrue(String code);
}
