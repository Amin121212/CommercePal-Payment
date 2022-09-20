package com.commerce.pal.payment.controller.user.merchant;

import com.commerce.pal.payment.model.payment.MerchantWithdrawal;
import com.commerce.pal.payment.module.ValidateAccessToken;
import com.commerce.pal.payment.repo.payment.MerchantWithdrawalRepository;
import com.commerce.pal.payment.util.GlobalMethods;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Log
@CrossOrigin(origins = {"*"}, maxAge = 3600L)
@RestController
@RequestMapping({"/prime/api/v1/merchant/transaction"})
@SuppressWarnings("Duplicates")
public class MerchantTransactionController {
    private final GlobalMethods globalMethods;
    private final ValidateAccessToken validateAccessToken;
    private final MerchantWithdrawalRepository merchantWithdrawalRepository;

    @Autowired
    public MerchantTransactionController(GlobalMethods globalMethods,
                                         ValidateAccessToken validateAccessToken,
                                         MerchantWithdrawalRepository merchantWithdrawalRepository) {
        this.globalMethods = globalMethods;
        this.validateAccessToken = validateAccessToken;
        this.merchantWithdrawalRepository = merchantWithdrawalRepository;
    }
}
