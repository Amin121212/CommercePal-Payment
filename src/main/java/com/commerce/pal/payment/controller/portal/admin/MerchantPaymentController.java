package com.commerce.pal.payment.controller.portal.admin;

import com.commerce.pal.payment.integ.payment.ethio.EthioFundsTransfer;
import com.commerce.pal.payment.integ.payment.sahay.SahayTransfer;
import com.commerce.pal.payment.module.DataAccessService;
import com.commerce.pal.payment.module.database.PaymentStoreProcedure;
import com.commerce.pal.payment.repo.payment.MerchantWithdrawalRepository;
import com.commerce.pal.payment.util.ResponseCodes;
import com.commerce.pal.payment.util.specification.SpecificationsDao;
import com.commerce.pal.payment.util.specification.utils.SearchCriteria;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

@Log
@CrossOrigin(origins = {"*"}, maxAge = 3600L)
@RestController
@RequestMapping({"/prime/api/v1/portal/merchant/payments"})
@SuppressWarnings("Duplicates")
public class MerchantPaymentController {
    private final SahayTransfer sahayTransfer;
    private final DataAccessService dataAccessService;
    private final SpecificationsDao specificationsDao;
    private final EthioFundsTransfer ethioFundsTransfer;
    private final PaymentStoreProcedure paymentStoreProcedure;
    private final MerchantWithdrawalRepository merchantWithdrawalRepository;

    @Autowired
    public MerchantPaymentController(SahayTransfer sahayTransfer,
                                     DataAccessService dataAccessService,
                                     SpecificationsDao specificationsDao,
                                     EthioFundsTransfer ethioFundsTransfer,
                                     PaymentStoreProcedure paymentStoreProcedure,
                                     MerchantWithdrawalRepository merchantWithdrawalRepository) {
        this.sahayTransfer = sahayTransfer;
        this.dataAccessService = dataAccessService;
        this.specificationsDao = specificationsDao;
        this.ethioFundsTransfer = ethioFundsTransfer;
        this.paymentStoreProcedure = paymentStoreProcedure;
        this.merchantWithdrawalRepository = merchantWithdrawalRepository;
    }

    //Admin Configure Shipping process and the respective WareHouse
    @RequestMapping(value = "/withdrawals", method = RequestMethod.GET)
    public ResponseEntity<?> merchantWithdrawals(@RequestParam("MerchantId") Optional<String> MerchantId) {
        JSONObject responseMap = new JSONObject();
        try {
            List<JSONObject> list = new ArrayList<>();
            List<SearchCriteria> params = new ArrayList<SearchCriteria>();
            MerchantId.ifPresent(value -> {
                params.add(new SearchCriteria("merchantId", ":", MerchantId));
            });
            specificationsDao.getMerchantWithdrawal(params).
                    forEach(merchantWithdrawal -> {
                        JSONObject merBdy = new JSONObject();
                        JSONObject merReq = new JSONObject();
                        merReq.put("Type", "MERCHANT");
                        merReq.put("TypeId", merchantWithdrawal.getMerchantId());
                        JSONObject merRes = dataAccessService.pickAndProcess(merReq);
                        merBdy.put("MerchantInfo", merRes);
                        merBdy.put("RequestId", merchantWithdrawal.getId());
                        merBdy.put("TransRef", merchantWithdrawal.getTransRef());
                        merBdy.put("WithdrawalMethod", merchantWithdrawal.getWithdrawalMethod());
                        merBdy.put("WithdrawalType", merchantWithdrawal.getWithdrawalType());
                        merBdy.put("Account", merchantWithdrawal.getAccount());
                        merBdy.put("Amount", merchantWithdrawal.getAmount());
                        merBdy.put("RequestDate", merchantWithdrawal.getRequestDate());
                        merBdy.put("VerifiedBy", merchantWithdrawal.getVerifiedBy());
                        merBdy.put("VerificationDate", merchantWithdrawal.getVerificationDate());
                        merBdy.put("ResponseStatus", merchantWithdrawal.getResponseStatus());
                        merBdy.put("ResponseDescription", merchantWithdrawal.getResponseDescription());
                        merBdy.put("ResponseDate", merchantWithdrawal.getResponseDate());
                        list.add(merBdy);
                    });

            responseMap.put("statusCode", ResponseCodes.SUCCESS)
                    .put("statusDescription", "Request Successful")
                    .put("data", list)
                    .put("statusMessage", "Request Successful");
        } catch (Exception ex) {
            responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                    .put("statusDescription", ex.getMessage())
                    .put("statusMessage", ex.getMessage());
            log.log(Level.WARNING, ex.getMessage());
        }
        return ResponseEntity.ok(responseMap.toString());
    }

    @RequestMapping(value = "/withdrawal-approval", method = RequestMethod.POST)
    public ResponseEntity<?> withdrawalApproval(@RequestBody String req) {
        AtomicReference<JSONObject> responseMap = new AtomicReference<>(new JSONObject());
        try {
            JSONObject request = new JSONObject(req);
            merchantWithdrawalRepository.findById(request.getLong("RequestId"))
                    .ifPresentOrElse(merchantWithdrawal -> {
                        merchantWithdrawal.setStatus(request.getInt("Status"));
                        merchantWithdrawal.setVerificationComment(request.getString("Comment"));
                        merchantWithdrawal.setVerificationDate(Timestamp.from(Instant.now()));
                        merchantWithdrawal.setVerifiedBy(request.getInt("UserId"));
                        merchantWithdrawalRepository.save(merchantWithdrawal);
                        if (merchantWithdrawal.getStatus().equals(3)) {
                            JSONObject merReq = new JSONObject();
                            merReq.put("Type", "MERCHANT");
                            merReq.put("TypeId", merchantWithdrawal.getMerchantId());
                            JSONObject merRes = dataAccessService.pickAndProcess(merReq);

                            String payNar = "Withdrawal of Trans Ref [" + merchantWithdrawal.getTransRef() + "]";
                            JSONObject reqBody = new JSONObject();
                            reqBody.put("TransRef", merchantWithdrawal.getTransRef());
                            reqBody.put("MerchantEmail", merRes.getString("email"));
                            reqBody.put("Currency", "ETB");
                            reqBody.put("Amount", merchantWithdrawal.getAmount().toString());
                            reqBody.put("WithdrawalMethod", merchantWithdrawal.getWithdrawalMethod());
                            reqBody.put("PaymentNarration", payNar);
                            JSONObject payRes = paymentStoreProcedure.merchantWithdrawal(reqBody);
                            if (payRes.getString("Status").equals("00")) {
                                switch (merchantWithdrawal.getWithdrawalMethod()) {
                                    case "SAHAY-SWFT":
                                        responseMap.set(sahayTransfer.pickAndProcess(merchantWithdrawal));
                                        break;
                                    case "SAHAY-SESFT":
                                        responseMap.set(ethioFundsTransfer.pickAndProcess(merchantWithdrawal));
                                        break;
                                }

                            } else {
                                merchantWithdrawal.setBillTransRef("Failed");
                                merchantWithdrawal.setResponsePayload(payRes.toString());
                                merchantWithdrawal.setResponseStatus(1);
                                merchantWithdrawal.setResponseDescription(payRes.getString("Message"));
                                merchantWithdrawal.setResponseDate(Timestamp.from(Instant.now()));
                                merchantWithdrawalRepository.save(merchantWithdrawal);
                            }
                        } else {
                            merchantWithdrawal.setBillTransRef("Rejected");
                            merchantWithdrawal.setResponsePayload(request.getString("Comment"));
                            merchantWithdrawal.setResponseStatus(1);
                            merchantWithdrawal.setResponseDescription(request.getString("Comment"));
                            merchantWithdrawal.setResponseDate(Timestamp.from(Instant.now()));
                            merchantWithdrawalRepository.save(merchantWithdrawal);
                            responseMap.get().put("statusCode", ResponseCodes.SUCCESS)
                                    .put("statusDescription", "Request Successful")
                                    .put("statusMessage", "Request Successful");
                        }
                    }, () -> {
                        responseMap.get().put("statusCode", ResponseCodes.REQUEST_FAILED)
                                .put("statusDescription", "Failed")
                                .put("statusMessage", "Failed");
                    });

        } catch (Exception ex) {
            responseMap.get().put("statusCode", ResponseCodes.REQUEST_FAILED)
                    .put("statusDescription", ex.getMessage())
                    .put("statusMessage", ex.getMessage());
            log.log(Level.WARNING, ex.getMessage());
        }
        return ResponseEntity.ok(responseMap.toString());
    }


}
