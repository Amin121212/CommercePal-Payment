package com.commerce.pal.payment.integ.payment.cbebirr;

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
public class CBEBirrPayment {

    @Value(value = "${org.cbe.birr.payment.url}")
    private String URL_PAYMENT_REQUEST;
    private final PalPaymentRepository palPaymentRepository;
    private final CBEBirrPaymentService cbeBirrPaymentService;

    private final HttpProcessor httpProcessor;

    @Autowired
    public CBEBirrPayment(PalPaymentRepository palPaymentRepository, CBEBirrPaymentService cbeBirrPaymentService, HttpProcessor httpProcessor) {
        this.palPaymentRepository = palPaymentRepository;
        this.cbeBirrPaymentService = cbeBirrPaymentService;
        this.httpProcessor = httpProcessor;
    }

    public JSONObject pickAndProcess(PalPayment payment) {
        JSONObject respBdy = new JSONObject();
        try {
            String requestParameters = cbeBirrPaymentService.generatePaymentUrl(
                    String.valueOf(payment.getAmount()), payment.getTransRef());

            JSONObject reqBdy = new JSONObject();
            reqBdy.put("amount", payment.getAmount().toString())
                    .put("transRef", payment.getTransRef());

            RequestBuilder builder = new RequestBuilder("POST");
            builder.addHeader("Content-Type", "application/json")
                    .setBody(reqBdy.toString())
                    .setUrl("http://localhost:2030/payment/v1/cbe-birr/request-parameters")
                    .build();

            JSONObject resp = httpProcessor.jsonRequestProcessor(builder);
            if (resp.getString("StatusCode").equals("200")) {

                JSONObject resBody = new JSONObject(resp.getString("ResponseBody"));
//                String requestParameters = resBody.getString("requestParameters");

                payment.setRequestPayload(reqBdy.toString());
                palPaymentRepository.save(payment);

                String paymentUrl = URL_PAYMENT_REQUEST + requestParameters;
                respBdy.put("statusCode", ResponseCodes.SUCCESS)
                        .put("OrderRef", payment.getOrderRef())
                        .put("TransRef", payment.getTransRef())
                        .put("PaymentUrl", paymentUrl)
                        .put("statusDescription", "Success")
                        .put("statusMessage", "Success");

                payment.setStatus(1);
                payment.setBillTransRef(payment.getTransRef());
                payment.setFinalResponse("0");
                payment.setFinalResponseMessage("PENDING");
                payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                palPaymentRepository.save(payment);
            } else {
                respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                        .put("PaymentUrl", "")
                        .put("statusDescription", "Request failed")
                        .put("statusMessage", "FAILED");


                payment.setStatus(5);
                payment.setBillTransRef("FAILED");
                payment.setResponsePayload("FAILED");
                payment.setResponseDate(Timestamp.from(Instant.now()));

                payment.setFinalResponse("999");
                payment.setFinalResponseMessage("FAILED");
                payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                palPaymentRepository.save(payment);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log.log(Level.WARNING, ex.getMessage());
            respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("PaymentUrl", "")
                    .put("statusDescription", "failed")
                    .put("statusMessage", "Request failed");

            payment.setStatus(5);
            payment.setBillTransRef("FAILED");
            payment.setFinalResponse("999");
            payment.setFinalResponseMessage("FAILED");
            payment.setFinalResponseDate(Timestamp.from(Instant.now()));
            palPaymentRepository.save(payment);
        }

        return respBdy;
    }
}

