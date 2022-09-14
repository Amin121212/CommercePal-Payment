package com.commerce.pal.payment.repo.payment;

import com.commerce.pal.payment.model.payment.MerchantWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantWithdrawalRepository extends JpaRepository<MerchantWithdrawal, Long> {
}
