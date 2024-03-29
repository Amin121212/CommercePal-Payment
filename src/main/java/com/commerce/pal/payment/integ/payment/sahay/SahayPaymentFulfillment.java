package com.commerce.pal.payment.integ.payment.sahay;

import com.commerce.pal.payment.module.payment.ProcessSuccessPayment;
import com.commerce.pal.payment.repo.payment.PalPaymentRepository;
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
public class SahayPaymentFulfillment {
    @Value(value = "${org.commerce.pal.sahay.payment.fulfillment.endpoint}")
    private String URL_PAYMENT_FULFILLMENT;

    private final SahayConstants sahayConstants;
    private final HttpProcessor httpProcessor;
    private final PalPaymentRepository palPaymentRepository;
    private final ProcessSuccessPayment processSuccessPayment;

    @Autowired
    public SahayPaymentFulfillment(SahayConstants sahayConstants,
                                   HttpProcessor httpProcessor,
                                   PalPaymentRepository palPaymentRepository,
                                   ProcessSuccessPayment processSuccessPayment) {
        this.sahayConstants = sahayConstants;
        this.httpProcessor = httpProcessor;
        this.palPaymentRepository = palPaymentRepository;
        this.processSuccessPayment = processSuccessPayment;
    }

    public JSONObject pickAndProcess(JSONObject reqBdy) {
        JSONObject respBdy = new JSONObject();
        try {
            palPaymentRepository.findPalPaymentByOrderRefAndTransRefAndStatus(
                    reqBdy.getString("OrderRef"), reqBdy.getString("TransRef"), 1
            ).ifPresentOrElse(payment -> {
                String accessToken = sahayConstants.getToken();
                if (accessToken == null || accessToken.equals("") || accessToken.contains("Error")) {
                    log.log(Level.SEVERE, "Unable to get access token");
                    respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                            .put("statusDescription", "failed")
                            .put("statusMessage", "Request failed");
                } else {
                    JSONObject payload = new JSONObject();
                    payload.put("TransRef", payment.getBillTransRef());
                    payload.put("BillerReference", payment.getTransRef());
                    payload.put("Code", reqBdy.getString("otp"));
                    payload.put("AccountType", "SAHAY");

                    payment.setRequestPayload(payload.toString());
                    palPaymentRepository.save(payment);

                    RequestBuilder builder = new RequestBuilder("POST");
                    builder.addHeader("Authorization", "Bearer " + accessToken)
                            .addHeader("Content-Type", "application/json")
                            .setBody(payload.toString())
                            .setUrl(URL_PAYMENT_FULFILLMENT)
                            .build();

                    JSONObject resp = httpProcessor.jsonRequestProcessor(builder);
                    log.log(Level.INFO, resp.toString());

                    if (resp.getString("StatusCode").equals("200")) {
                        JSONObject resBody = new JSONObject(resp.getString("ResponseBody"));
                        payment.setResponsePayload(resBody.toString());
                        payment.setResponseDate(Timestamp.from(Instant.now()));
                        if (resBody.getString("response").equals("000")) {
                            respBdy.put("statusCode", ResponseCodes.SUCCESS)
                                    .put("sahayRef", payment.getOrderRef())
                                    .put("TransRef", payment.getTransRef())
                                    .put("statusDescription", "Success")
                                    .put("statusMessage", "Success");

                            payment.setStatus(3);
                            payment.setFinalResponse("000");
                            payment.setFinalResponseMessage(resBody.getString("responseDescription"));
                            payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                            palPaymentRepository.save(payment);

                            // Process Payment
                            processSuccessPayment.pickAndProcess(payment);
                        } else {
                            respBdy.put("statusCode", ResponseCodes.TRANSACTION_FAILED)
                                    .put("statusDescription", "failed")
                                    .put("statusMessage", "Request failed");

                            payment.setStatus(5);
                            payment.setFinalResponse("999");
                            payment.setFinalResponseMessage(resBody.getString("responseDescription"));
                            payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                            palPaymentRepository.save(payment);
                        }
                    } else {
                        respBdy.put("statusCode", ResponseCodes.TRANSACTION_FAILED)
                                .put("statusDescription", "failed")
                                .put("statusMessage", "Request failed");
                    }
                }
            }, () -> {
                respBdy.put("statusCode", ResponseCodes.TRANSACTION_FAILED)
                        .put("statusDescription", "failed")
                        .put("statusMessage", "Request failed");
            });
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
            respBdy.put("statusCode", ResponseCodes.TRANSACTION_FAILED)
                    .put("statusDescription", "failed")
                    .put("statusMessage", ex.getMessage());
        }
        return respBdy;
    }
}
