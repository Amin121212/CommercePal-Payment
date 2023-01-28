package com.commerce.pal.payment.integ.payment.sahay;

import com.commerce.pal.payment.model.payment.PalPayment;
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
public class SahayPayment {

    @Value(value = "${org.commerce.pal.sahay.payment.request.endpoint}")
    private String URL_PAYMENT_REQUEST;


    private final SahayConstants sahayConstants;
    private final HttpProcessor httpProcessor;
    private final PalPaymentRepository palPaymentRepository;

    @Autowired
    public SahayPayment(SahayConstants sahayConstants,
                        HttpProcessor httpProcessor,
                        PalPaymentRepository palPaymentRepository) {
        this.sahayConstants = sahayConstants;
        this.httpProcessor = httpProcessor;
        this.palPaymentRepository = palPaymentRepository;
    }

    public JSONObject pickAndProcess(PalPayment payment) {
        JSONObject respBdy = new JSONObject();
        try {
            String accessToken = sahayConstants.getToken();
            if (accessToken == null || accessToken.equals("") || accessToken.contains("Error")) {
                log.log(Level.SEVERE, "Unable to get access token");
                respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                        .put("statusDescription", "failed")
                        .put("statusMessage", "Request failed");

                payment.setStatus(5);
                payment.setBillTransRef("FAILED");
                payment.setResponsePayload("FAILED");
                payment.setResponseDate(Timestamp.from(Instant.now()));

                payment.setFinalResponse("999");
                payment.setFinalResponseMessage("FAILED");
                payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                palPaymentRepository.save(payment);
            } else {
                JSONObject payload = new JSONObject();
                payload.put("PhoneNumber", payment.getAccountNumber());
                payload.put("BillerReference", payment.getTransRef());
                payload.put("Amount", payment.getAmount().toString());

                payment.setRequestPayload(payload.toString());
                palPaymentRepository.save(payment);

                RequestBuilder builder = new RequestBuilder("POST");
                builder.addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Content-Type", "application/json")
                        .setBody(payload.toString())
                        .setUrl(URL_PAYMENT_REQUEST)
                        .build();

                JSONObject resp = httpProcessor.jsonRequestProcessor(builder);

                if (resp.getString("StatusCode").equals("200")) {
                    JSONObject resBody = new JSONObject(resp.getString("ResponseBody"));
                    payment.setResponsePayload(resBody.toString());
                    payment.setResponseDate(Timestamp.from(Instant.now()));
                    if (resBody.getString("response").equals("000")) {
                        respBdy.put("statusCode", ResponseCodes.SUCCESS)
                                .put("OrderRef", payment.getOrderRef())
                                .put("TransRef", payment.getTransRef())
                                .put("statusDescription", "Success")
                                .put("statusMessage", "Success");

                        payment.setStatus(1);
                        payment.setBillTransRef(resBody.getString("sahayRef"));
                        payment.setFinalResponse("0");
                        payment.setFinalResponseMessage("PENDING");
                        payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                        palPaymentRepository.save(payment);

                    } else {
                        respBdy.put("statusCode", ResponseCodes.NOT_EXIST)
                                .put("statusDescription", "failed")
                                .put("statusMessage", "Request failed");

                        payment.setStatus(5);
                        payment.setBillTransRef("FAILED");
                        payment.setFinalResponse("999");
                        payment.setFinalResponseMessage("FAILED");
                        payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                        palPaymentRepository.save(payment);
                    }
                } else {
                    respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                            .put("statusDescription", "failed")
                            .put("statusMessage", "Request failed");

                    payment.setStatus(5);
                    payment.setBillTransRef("FAILED");
                    payment.setResponsePayload("FAILED");
                    payment.setResponseDate(Timestamp.from(Instant.now()));

                    payment.setFinalResponse("999");
                    payment.setFinalResponseMessage("FAILED");
                    payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                    palPaymentRepository.save(payment);
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
