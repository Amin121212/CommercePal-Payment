package com.commerce.pal.payment.integ.payment.cbeBirrMiniApp;

import com.commerce.pal.payment.module.payment.ProcessSuccessPayment;
import com.commerce.pal.payment.repo.payment.PalPaymentRepository;
import com.commerce.pal.payment.util.HttpProcessor;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Level;

@Log
@Component
public class CBEBirrMiniAppPaymentFulfillment {
    @Value(value = "${org.cbe.birr.min.app.payment.user}")
    private String AUTH_URL;
    private final PalPaymentRepository palPaymentRepository;
    private final ProcessSuccessPayment processSuccessPayment;
    private final HttpProcessor httpProcessor;
    private final CBEBirrMiniAppPaymentService cbeBirrMiniAppPaymentService;

    @Autowired
    public CBEBirrMiniAppPaymentFulfillment(PalPaymentRepository palPaymentRepository,
                                            ProcessSuccessPayment processSuccessPayment,
                                            HttpProcessor httpProcessor,
                                            CBEBirrMiniAppPaymentService cbeBirrMiniAppPaymentService) {
        this.palPaymentRepository = palPaymentRepository;
        this.processSuccessPayment = processSuccessPayment;
        this.httpProcessor = httpProcessor;
        this.cbeBirrMiniAppPaymentService = cbeBirrMiniAppPaymentService;
    }

    public JSONObject pickAndProcess(String authorizationHeader, JSONObject reqBdy) {
        JSONObject respBdy = new JSONObject();
        try {
            palPaymentRepository.findPalPaymentByTransRefAndStatus(reqBdy.getString("transactionId"), 1)
                    .ifPresentOrElse(payment -> {
                        payment.setResponsePayload(reqBdy.toString());
                        payment.setResponseDate(Timestamp.from(Instant.now()));
                        palPaymentRepository.save(payment);

                        RequestBuilder builder = new RequestBuilder("GET");
                        builder.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                                .setHeader(HttpHeaders.ACCEPT, "application/json")
                                .setHeader(HttpHeaders.AUTHORIZATION, authorizationHeader)
                                .setUrl(AUTH_URL)
                                .build();

                        JSONObject resp = httpProcessor.jsonRequestProcessor(builder);

                        if (resp.getString("StatusCode").equals("200")) {
                            String transactionId = reqBdy.getString("transactionId");
                            String paidAmount = reqBdy.getString("paidAmount");
                            String transactionTime = reqBdy.getString("transactionTime");

                            String hashedValue = cbeBirrMiniAppPaymentService
                                    .generateHashForCbeBirr(paidAmount, transactionId, transactionTime);

                            if (reqBdy.getString("signature").equals(hashedValue)) {
                                respBdy.put("statusCode", ResponseCodes.SUCCESS)
                                        .put("statusMessage", "Success");

                                payment.setStatus(3);
                                payment.setFinalResponse("000");
                                payment.setFinalResponseMessage("SUCCESS");
                                payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                                palPaymentRepository.save(payment);

                                // Process Payment
                                processSuccessPayment.pickAndProcess(payment);
                            } else {
                                respBdy.put("statusCode", ResponseCodes.TRANSACTION_FAILED)
                                        .put("statusMessage", "Signature validation Failed");

                                payment.setStatus(5);
                                payment.setBillTransRef("FAILED");
                                payment.setFinalResponse("999");
                                payment.setFinalResponseMessage("FAILED DUE INVALID SIGNATURE");
                                payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                                palPaymentRepository.save(payment);
                            }

                        } else {

                            respBdy.put("statusCode", ResponseCodes.TRANSACTION_FAILED)
                                    .put("statusMessage", "Token authorization failed");
                        }
                    }, () -> respBdy.put("statusCode", ResponseCodes.TRANSACTION_FAILED)
                            .put("statusMessage", "Payment not found"));

        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
            respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusMessage", "Internal server error.");
        }
        return respBdy;
    }
}
