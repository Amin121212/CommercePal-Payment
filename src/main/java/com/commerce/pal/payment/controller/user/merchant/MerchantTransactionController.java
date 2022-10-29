package com.commerce.pal.payment.controller.user.merchant;

import com.commerce.pal.payment.integ.payment.ethio.EthioSwithAccount;
import com.commerce.pal.payment.integ.payment.sahay.SahayCustomerValidation;
import com.commerce.pal.payment.model.payment.MerchantWithdrawal;
import com.commerce.pal.payment.module.ValidateAccessToken;
import com.commerce.pal.payment.repo.payment.MerchantWithdrawalRepository;
import com.commerce.pal.payment.util.GlobalMethods;
import com.commerce.pal.payment.util.ResponseCodes;
import com.commerce.pal.payment.util.specification.SpecificationsDao;
import com.commerce.pal.payment.util.specification.utils.SearchCriteria;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

@Log
@CrossOrigin(origins = {"*"}, maxAge = 3600L)
@RestController
@RequestMapping({"/prime/api/v1/merchant/transaction"})
@SuppressWarnings("Duplicates")
public class MerchantTransactionController {
    private final GlobalMethods globalMethods;
    private final EthioSwithAccount ethioSwithAccount;
    private final SpecificationsDao specificationsDao;
    private final ValidateAccessToken validateAccessToken;
    private final SahayCustomerValidation sahayCustomerValidation;
    private final MerchantWithdrawalRepository merchantWithdrawalRepository;

    @Autowired
    public MerchantTransactionController(GlobalMethods globalMethods,
                                         EthioSwithAccount ethioSwithAccount,
                                         SpecificationsDao specificationsDao,
                                         ValidateAccessToken validateAccessToken,
                                         SahayCustomerValidation sahayCustomerValidation,
                                         MerchantWithdrawalRepository merchantWithdrawalRepository) {
        this.globalMethods = globalMethods;
        this.ethioSwithAccount = ethioSwithAccount;
        this.specificationsDao = specificationsDao;
        this.validateAccessToken = validateAccessToken;
        this.sahayCustomerValidation = sahayCustomerValidation;
        this.merchantWithdrawalRepository = merchantWithdrawalRepository;
    }

    @RequestMapping(value = {"/account-balance"}, method = {RequestMethod.GET}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> getAccountBalance(@RequestHeader("Authorization") String accessToken) {
        JSONObject responseMap = new JSONObject();
        JSONObject valTokenReq = new JSONObject();
        valTokenReq.put("AccessToken", accessToken)
                .put("UserType", "M");

        JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);

        if (valTokenBdy.getString("Status").equals("00")) {
            JSONObject userDetails = valTokenBdy.getJSONObject("UserDetails");

            if (userDetails.has("merchantInfo")) {
                JSONObject merchantInfo = userDetails.getJSONObject("merchantInfo");
                String balance = "0.00";
                balance = globalMethods.getAccountBalance(merchantInfo.getString("tillNumber"));
                responseMap.put("statusCode", ResponseCodes.SUCCESS)
                        .put("statusDescription", "success")
                        .put("balance", new BigDecimal(balance))
                        .put("statusMessage", "Request Successful");
            } else {
                responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                        .put("statusDescription", "Merchant Does not exists")
                        .put("statusMessage", "Merchant Does not exists");

            }
        } else {
            responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                    .put("statusDescription", "Merchant Does not exists")
                    .put("statusMessage", "Merchant Does not exists");
        }
        return ResponseEntity.ok(responseMap.toString());
    }

    @RequestMapping(value = "/request-withdrawal", method = RequestMethod.POST)
    public ResponseEntity<?> requestForWithdrawal(@RequestBody String req,
                                                  @RequestHeader("Authorization") String accessToken) {
        JSONObject responseMap = new JSONObject();
        try {
            JSONObject valTokenReq = new JSONObject();
            valTokenReq.put("AccessToken", accessToken)
                    .put("UserType", "M");

            JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);

            if (valTokenBdy.getString("Status").equals("00")) {
                JSONObject userDetails = valTokenBdy.getJSONObject("UserDetails");
                JSONObject request = new JSONObject(req);
                if (userDetails.has("merchantInfo")) {
                    JSONObject merchantInfo = userDetails.getJSONObject("merchantInfo");
                    String balance = "0.00";
                    balance = globalMethods.getAccountBalance(merchantInfo.getString("tillNumber"));
                    Long merchantId = Long.valueOf(userDetails.getJSONObject("merchantInfo").getInt("userId"));
                    if (Double.valueOf(balance) >= request.getBigDecimal("Amount").doubleValue()) {
                        merchantWithdrawalRepository.findMerchantWithdrawalByMerchantIdAndStatus(
                                        merchantId, 0)
                                .ifPresentOrElse(merchantWithdrawal -> {
                                    responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                                            .put("transRef", merchantWithdrawal.getTransRef())
                                            .put("statusDescription", "There is a pending request")
                                            .put("statusMessage", "There is a pending request");
                                }, () -> {
                                    JSONObject accountPayload = new JSONObject();
                                    switch (request.getString("WithdrawalMethod")) {
                                        case "SAHAY-SESFT":
                                            accountPayload = ethioSwithAccount.accountCheck(request);
                                            break;
                                        case "SAHAY-SWFT":
                                            accountPayload = sahayCustomerValidation.checkCustomer(request.getString("Account"));
                                            break;
                                    }
                                    if (accountPayload.getString("statusCode").equals(ResponseCodes.SUCCESS)) {
                                        accountPayload.put("PhoneNumber", merchantInfo.getString("ownerPhoneNumber"));

                                        String validationCode = globalMethods.generateValidationCode();
                                        String transRef = globalMethods.generateTrans();
                                        AtomicReference<MerchantWithdrawal> withdrawal = new AtomicReference<>(new MerchantWithdrawal());
                                        withdrawal.get().setMerchantId(merchantId);
                                        withdrawal.get().setTransRef(transRef);
                                        withdrawal.get().setWithdrawalMethod(request.getString("WithdrawalMethod"));
                                        withdrawal.get().setWithdrawalType(request.getString("WithdrawalType"));
                                        withdrawal.get().setAccountPayload(accountPayload.toString());
                                        withdrawal.get().setAccount(request.getString("Account"));
                                        withdrawal.get().setAmount(request.getBigDecimal("Amount"));
                                        withdrawal.get().setValidationCode(globalMethods.encryptCode(validationCode));
                                        withdrawal.get().setValidationDate(Timestamp.from(Instant.now()));
                                        withdrawal.get().setStatus(0);
                                        withdrawal.get().setRequestDate(Timestamp.from(Instant.now()));
                                        withdrawal.get().setResponseStatus(0);
                                        withdrawal.get().setBillTransRef("Failed");
                                        withdrawal.get().setResponsePayload("Pending");
                                        withdrawal.get().setResponseStatus(0);
                                        withdrawal.get().setResponseDescription("Pending");
                                        withdrawal.get().setResponseDate(Timestamp.from(Instant.now()));
                                        withdrawal.set(merchantWithdrawalRepository.save(withdrawal.get()));

                                        responseMap.put("statusCode", ResponseCodes.SUCCESS)
                                                .put("statusDescription", "success")
                                                .put("transRef", transRef)
                                                .put("accountPayload", accountPayload)
                                                .put("statusMessage", "Request Successful");
                                    } else {
                                        responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                                                .put("statusDescription", "Insufficient Balance")
                                                .put("statusMessage", "Insufficient Balance");
                                    }
                                });
                    } else {
                        responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                                .put("statusDescription", "Insufficient Balance")
                                .put("statusMessage", "Insufficient Balance");
                    }
                } else {
                    responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                            .put("statusDescription", "Merchant Does not exists")
                            .put("statusMessage", "Merchant Does not exists");

                }
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

    @RequestMapping(value = {"/transactions"}, method = {RequestMethod.GET}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> transactions(@RequestHeader("Authorization") String accessToken,
                                          @RequestParam("accountType") Optional<String> accountType,
                                          @RequestParam("pageNumber") Optional<Integer> pageNumber,
                                          @RequestParam("transType") Optional<String> transType,
                                          @RequestParam("transRef") Optional<String> transRef,
                                          @RequestParam("startDate") Optional<String> startDate,
                                          @RequestParam("endDate") Optional<String> endDate) {
        JSONObject responseMap = new JSONObject();
        try {

            JSONObject valTokenReq = new JSONObject();
            valTokenReq.put("AccessToken", accessToken)
                    .put("UserType", "M");

            JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);

            if (valTokenBdy.getString("Status").equals("00")) {
                JSONObject userDetails = valTokenBdy.getJSONObject("UserDetails");

                if (userDetails.has("merchantInfo")) {
                    JSONObject merchantInfo = userDetails.getJSONObject("merchantInfo");
                    AtomicReference<Integer> page = new AtomicReference<>(1);
                    List<SearchCriteria> params = new ArrayList<SearchCriteria>();

                    transType.ifPresent(value -> {
                        params.add(new SearchCriteria("transType", ":", value));
                    });
                    transRef.ifPresent(value -> {
                        params.add(new SearchCriteria("transRef", ":", value));
                    });
                    pageNumber.ifPresent(value -> {
                        page.set(value);
                    });
                    accountType.ifPresentOrElse(value -> {
                        params.add(new SearchCriteria("account", ":", value));
                    }, () -> {
                        params.add(new SearchCriteria("account", ":", merchantInfo.getString("tillNumber")));
                    });

                    startDate.ifPresent(value -> {
                        endDate.ifPresent(value2 -> {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                            try {
                                Date startDate1 = dateFormat.parse(value);
                                Date endDate1 = dateFormat.parse(value2);
                                params.add(new SearchCriteria("transDate", ">D", new Timestamp(startDate1.getTime())));
                                params.add(new SearchCriteria("transDate", "<D", new Timestamp(endDate1.getTime())));
                            } catch (ParseException e) {
                                log.log(Level.WARNING, e.getMessage());
                            }
                        });
                    });

                    List<JSONObject> transDetails = new ArrayList<>();
                    specificationsDao.getTransactions(params, page.get())
                            .forEach(transaction -> {
                                JSONObject accDetail = new JSONObject();
                                accDetail.put("Id", transaction.getId());
                                accDetail.put("TransRef", transaction.getTransRef());
                                accDetail.put("TransType", transaction.getTransType());
                                accDetail.put("Account", transaction.getAccount());
                                accDetail.put("DrCr", transaction.getDrCr());
                                accDetail.put("TransDate", transaction.getTransDate());
                                accDetail.put("Amount", transaction.getAmount());
                                accDetail.put("Currency", transaction.getCurrency());
                                accDetail.put("Narration", transaction.getNarration());
                                transDetails.add(accDetail);
                            });
                    responseMap.put("Status", "00");
                    responseMap.put("Message", "Success");
                    responseMap.put("List", transDetails);
                } else {
                    responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                            .put("statusDescription", "Merchant Does not exists")
                            .put("statusMessage", "Merchant Does not exists");

                }
            } else {
                responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                        .put("statusDescription", "Merchant Does not exists")
                        .put("statusMessage", "Merchant Does not exists");
            }
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
            responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                    .put("statusDescription", ex.getMessage())
                    .put("statusMessage", ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.OK).body(responseMap.toString());
    }
}
