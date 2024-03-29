package com.commerce.pal.payment.integ.payment.sahay;

import com.commerce.pal.payment.model.payment.MerchantWithdrawal;
import com.commerce.pal.payment.repo.payment.MerchantWithdrawalRepository;
import com.commerce.pal.payment.util.HttpProcessor;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class SahayTransfer {

    @Value(value = "${org.commerce.pal.sahay.payment.funds.transfer.endpoint}")
    private String URL_PAYMENT_REQUEST;

    private final SahayConstants sahayConstants;
    private final HttpProcessor httpProcessor;
    private final MerchantWithdrawalRepository merchantWithdrawalRepository;

    @Autowired
    public SahayTransfer(SahayConstants sahayConstants,
                         HttpProcessor httpProcessor,
                         MerchantWithdrawalRepository merchantWithdrawalRepository) {
        this.sahayConstants = sahayConstants;
        this.httpProcessor = httpProcessor;
        this.merchantWithdrawalRepository = merchantWithdrawalRepository;
    }

    public JSONObject pickAndProcess(MerchantWithdrawal merchantWithdrawal) {
        JSONObject respBdy = new JSONObject();
        try {
            String accessToken = sahayConstants.getToken();
            if (accessToken == null || accessToken.equals("") || accessToken.contains("Error")) {
                log.log(Level.SEVERE, "Unable to get access token");
                respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                        .put("statusDescription", "Unable to get access token")
                        .put("statusMessage", "Unable to get access token");

                merchantWithdrawal.setBillTransRef("FAILED");
                merchantWithdrawal.setResponsePayload("FAILED");
                merchantWithdrawal.setResponseStatus(5);
                merchantWithdrawal.setResponseDescription("FAILED");
                merchantWithdrawal.setResponseDate(Timestamp.from(Instant.now()));
                merchantWithdrawalRepository.save(merchantWithdrawal);
            } else {
                JSONObject payload = new JSONObject();
                payload.put("PhoneNumber", merchantWithdrawal.getAccount());
                payload.put("BillerReference", merchantWithdrawal.getTransRef());
                payload.put("Amount", merchantWithdrawal.getAmount().toString());
                payload.put("AccountNumber", merchantWithdrawal.getAccount());
                payload.put("AccountType", "SAHAY");

                RequestBuilder builder = new RequestBuilder("POST");
                builder.addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Content-Type", "application/json")
                        .setBody(payload.toString())
                        .setUrl(URL_PAYMENT_REQUEST)
                        .build();

                JSONObject resp = httpProcessor.jsonRequestProcessor(builder);

                if (resp.getString("StatusCode").equals("200")) {
                    JSONObject resBody = new JSONObject(resp.getString("ResponseBody"));
                    merchantWithdrawal.setResponsePayload(resBody.toString());
                    merchantWithdrawal.setResponseDate(Timestamp.from(Instant.now()));
                    if (resBody.getString("response").equals("000")) {
                        respBdy.put("statusCode", ResponseCodes.SUCCESS)
                                .put("BillRef", resBody.getString("sahayRef"))
                                .put("statusDescription", "Success")
                                .put("statusMessage", "Success");

                        merchantWithdrawal.setBillTransRef(resBody.getString("sahayRef"));
                        merchantWithdrawal.setResponsePayload(resBody.toString());
                        merchantWithdrawal.setResponseStatus(3);
                        merchantWithdrawal.setResponseDescription("Success");
                        merchantWithdrawal.setResponseDate(Timestamp.from(Instant.now()));
                        merchantWithdrawalRepository.save(merchantWithdrawal);

                    } else {
                        //todo
                        //Auto reversal
                        respBdy.put("statusCode", ResponseCodes.NOT_EXIST)
                                .put("statusDescription", "failed")
                                .put("statusMessage", "Request failed");
                        merchantWithdrawal.setBillTransRef("FAILED");
                        merchantWithdrawal.setResponsePayload(resBody.toString());
                        merchantWithdrawal.setResponseStatus(5);
                        merchantWithdrawal.setResponseDescription("FAILED");
                        merchantWithdrawal.setResponseDate(Timestamp.from(Instant.now()));
                        merchantWithdrawalRepository.save(merchantWithdrawal);
                    }
                } else {
                    respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                            .put("statusDescription", "failed")
                            .put("statusMessage", "Request failed");

                    merchantWithdrawal.setBillTransRef("FAILED");
                    merchantWithdrawal.setResponsePayload("FAILED");
                    merchantWithdrawal.setResponseStatus(5);
                    merchantWithdrawal.setResponseDescription("FAILED");
                    merchantWithdrawal.setResponseDate(Timestamp.from(Instant.now()));
                    merchantWithdrawalRepository.save(merchantWithdrawal);
                }
            }
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
            respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed")
                    .put("statusMessage", "Request failed");
        }
        return respBdy;
    }
}
