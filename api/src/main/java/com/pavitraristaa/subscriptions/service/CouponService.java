package com.pavitraristaa.subscriptions.service;

import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.subscriptions.dto.CouponValidationResponse;
import com.pavitraristaa.subscriptions.dto.ValidateCouponRequest;
import com.pavitraristaa.subscriptions.entity.Coupon;
import com.pavitraristaa.subscriptions.entity.CouponDiscountType;
import com.pavitraristaa.subscriptions.repository.CouponRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    /** The standalone POST /coupons/validate endpoint - exploratory, always 200, never throws on a bad code. */
    @Transactional(readOnly = true)
    public CouponValidationResponse validate(ValidateCouponRequest request, BigDecimal planPrice) {
        Optional<Coupon> found = couponRepository.findByCodeIgnoreCaseAndActiveTrue(request.couponCode());
        if (found.isEmpty()) {
            return invalid(request.couponCode(), "Unknown or inactive coupon code");
        }
        Coupon coupon = found.get();
        String reason = validityIssue(coupon, request.planCode());
        if (reason != null) {
            return invalid(coupon.getCode(), reason);
        }
        BigDecimal discounted = applyDiscount(coupon, planPrice);
        return new CouponValidationResponse(
                true, coupon.getCode(), coupon.getDiscountType().name(), coupon.getDiscountValue(), planPrice, discounted, null);
    }

    /**
     * Looked up and validated again at checkout (never trust a client-supplied "it was valid a moment ago") -
     * throws instead of returning a soft "invalid" result, since submitting a checkout should fail hard on bad
     * input. Also increments redemption_count; there is no per-subscription coupon reference column on the
     * approved schema (see V10's migration comment), so only the running total against max_redemptions is
     * tracked, not which specific subscription used it.
     */
    @Transactional
    public BigDecimal requireAndRedeem(String couponCode, String planCode, BigDecimal planPrice) {
        Coupon coupon = couponRepository.findByCodeIgnoreCaseAndActiveTrue(couponCode)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "Unknown or inactive coupon code"));
        String reason = validityIssue(coupon, planCode);
        if (reason != null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, reason);
        }
        coupon.setRedemptionCount(coupon.getRedemptionCount() + 1);
        couponRepository.save(coupon);
        return applyDiscount(coupon, planPrice);
    }

    private String validityIssue(Coupon coupon, String planCode) {
        Instant now = Instant.now();
        if (coupon.getStartsAt() != null && now.isBefore(coupon.getStartsAt())) {
            return "Coupon is not active yet";
        }
        if (coupon.getExpiresAt() != null && now.isAfter(coupon.getExpiresAt())) {
            return "Coupon has expired";
        }
        if (coupon.getMaxRedemptions() != null && coupon.getRedemptionCount() >= coupon.getMaxRedemptions()) {
            return "Coupon usage limit has been reached";
        }
        if (coupon.getApplicablePlanCode() != null && !coupon.getApplicablePlanCode().equalsIgnoreCase(planCode)) {
            return "Coupon is not valid for this plan";
        }
        return null;
    }

    private BigDecimal applyDiscount(Coupon coupon, BigDecimal price) {
        BigDecimal discounted = coupon.getDiscountType() == CouponDiscountType.PERCENTAGE
                ? price.subtract(price.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                : price.subtract(coupon.getDiscountValue());
        return discounted.max(BigDecimal.ZERO);
    }

    private CouponValidationResponse invalid(String code, String reason) {
        return new CouponValidationResponse(false, code, null, null, null, null, reason);
    }
}
