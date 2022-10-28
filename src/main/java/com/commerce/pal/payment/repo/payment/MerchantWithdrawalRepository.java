package com.commerce.pal.payment.repo.payment;

import com.commerce.pal.payment.model.payment.MerchantWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantWithdrawalRepository extends JpaRepository<MerchantWithdrawal, Long> {

    Optional<MerchantWithdrawal> findMerchantWithdrawalByMerchantIdAndStatus(Long merchant, Integer status);

    Optional<MerchantWithdrawal> findMerchantWithdrawalByIdAndMerchantId(Long id, Long merchant);
}
