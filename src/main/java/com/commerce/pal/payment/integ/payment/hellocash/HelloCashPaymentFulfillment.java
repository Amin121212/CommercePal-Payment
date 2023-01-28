package com.commerce.pal.payment.integ.payment.hellocash;

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
public class HelloCashPaymentFulfillment {
    @Value(value = "${org.hello.cash.validate.payment}")
    private String URL_PAYMENT_FULFILLMENT;

    private final HelloCashConstants helloCashConstants;
    private final HttpProcessor httpProcessor;
    private final PalPaymentRepository palPaymentRepository;
    private final ProcessSuccessPayment processSuccessPayment;

    @Autowired
    public HelloCashPaymentFulfillment(HelloCashConstants helloCashConstants,
                                       HttpProcessor httpProcessor,
                                       PalPaymentRepository palPaymentRepository,
                                       ProcessSuccessPayment processSuccessPayment) {
        this.helloCashConstants = helloCashConstants;
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
                String accessToken = helloCashConstants.getToken();
                if (accessToken == null || accessToken.equals("") || accessToken.contains("Error")) {
                    log.log(Level.SEVERE, "Unable to get access token");
                    respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                            .put("statusDescription", "failed")
                            .put("statusMessage", "Request failed");
                } else {
                    String requestPayload = URL_PAYMENT_FULFILLMENT + payment.getBillTransRef();

                    payment.setRequestPayload(requestPayload);
                    palPaymentRepository.save(payment);

                    RequestBuilder builder = new RequestBuilder("GET");
                    builder.addHeader("Authorization", "Bearer " + accessToken)
                            .addHeader("Content-Type", "application/json")
                            .setUrl(requestPayload)
                            .build();

                    JSONObject resp = httpProcessor.jsonRequestProcessor(builder);
                    log.log(Level.INFO, resp.toString());

                    if (resp.getString("StatusCode").equals("200")) {
                        JSONObject resBody = new JSONObject(resp.getString("ResponseBody"));
                        payment.setResponsePayload(resBody.toString());
                        payment.setResponseDate(Timestamp.from(Instant.now()));
                        if (resBody.getString("status").equals("PROCESSED")) {
                            respBdy.put("statusCode", ResponseCodes.SUCCESS)
                                    .put("billRef", payment.getOrderRef())
                                    .put("TransRef", payment.getTransRef())
                                    .put("statusDescription", "Success")
                                    .put("statusMessage", "Success");

                            payment.setStatus(3);
                            payment.setFinalResponse("000");
                            payment.setFinalResponseMessage(resBody.getString("status"));
                            payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                            palPaymentRepository.save(payment);

                            // Process Payment
                            processSuccessPayment.pickAndProcess(payment);
                        } else {
                            respBdy.put("statusCode", ResponseCodes.TRANSACTION_STILL_PENDING)
                                    .put("statusDescription", "Still Pending")
                                    .put("statusMessage", "Still Pending");

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
