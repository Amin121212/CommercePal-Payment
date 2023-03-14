package com.commerce.pal.payment.repo.payment;

import com.commerce.pal.payment.model.payment.PromotionPhone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromotionPhoneRepository extends JpaRepository<PromotionPhone, Long> {

    Optional<PromotionPhone> findPromotionPhoneByPhoneAndDeviceId(String phone, String device);
}
