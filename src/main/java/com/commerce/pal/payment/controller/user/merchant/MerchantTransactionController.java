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


    @RequestMapping(value = "/request-withdrawal", method = RequestMethod.POST)
    public ResponseEntity<?> requestForWithdrawal(@RequestBody String req,
                                                  @RequestHeader("Authorization") String accessToken) {
        JSONObject responseMap = new JSONObject();
        try {
            JSONObject request = new JSONObject(req);
            JSONObject valTokenReq = new JSONObject();
            valTokenReq.put("AccessToken", accessToken)
                    .put("UserType", "M");

            JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);

            if (valTokenBdy.getString("Status").equals("00")) {
                JSONObject userDetails = valTokenBdy.getJSONObject("UserDetails");
                JSONObject merchantInfo = userDetails.getJSONObject("merchantInfo");
                Long merchantId = Long.valueOf(merchantInfo.getInt("userId"));
                String validationCode = globalMethods.generateValidationCode();
                String transRef = globalMethods.generateTrans();
                AtomicReference<MerchantWithdrawal> withdrawal = new AtomicReference<>(new MerchantWithdrawal());
                withdrawal.get().setMerchantId(merchantId);
                withdrawal.get().setTransRef(transRef);
                withdrawal.get().setWithdrawalMethod(request.getString("WithdrawalMethod"));
                withdrawal.get().setWithdrawalType(request.getString("WithdrawalType"));
                withdrawal.get().setAccount(request.getString("Account"));
                withdrawal.get().setAmount(request.getBigDecimal("Amount"));
                withdrawal.get().setValidationCode(globalMethods.encryptCode(validationCode));
                withdrawal.get().setValidationDate(Timestamp.from(Instant.now()));
                withdrawal.get().setStatus(0);
                withdrawal.get().setRequestDate(Timestamp.from(Instant.now()));
                withdrawal.get().setResponseStatus(0);
                withdrawal.set(merchantWithdrawalRepository.save(withdrawal.get()));

                JSONObject merchWth = new JSONObject();
                merchWth.put("TransRef", transRef);

                responseMap.put("statusCode", ResponseCodes.SUCCESS)
                        .put("statusDescription", "success")
                        .put("transRef", transRef)
                        .put("statusMessage", "Request Successful");
            } else {
                responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                        .put("statusDescription", "Merchant Does not exists")
                        .put("statusMessage", "Merchant Does not exists");
            }
        } catch (Exception e) {
            responseMap.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed to process request")
                    .put("statusMessage", "internal system error");
        }
        return ResponseEntity.status(HttpStatus.OK).body(responseMap.toString());
    }
}
