package com.commerce.pal.payment.integ.payment.epg;

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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class EPGPayment {

    @Value(value = "${org.epg.initiate.payment}")
    private String URL_PAYMENT_REQUEST;
    @Value(value = "${org.epg.userName}")
    private String userName;
    @Value(value = "${org.epg.password}")
    private String password;
    @Value(value = "${org.epg.returnUrl}")
    private String returnUrl;
    private final HttpProcessor httpProcessor;
    private final PalPaymentRepository palPaymentRepository;

    @Autowired
    public EPGPayment(HttpProcessor httpProcessor,
                      PalPaymentRepository palPaymentRepository) {
        this.httpProcessor = httpProcessor;
        this.palPaymentRepository = palPaymentRepository;
    }

    public JSONObject pickAndProcess(PalPayment payment) {
        JSONObject respBdy = new JSONObject();
        try {
            // Generate a numeric transaction reference as required by Ethio Switch.
            String transRef = NumericTransRefGenerator.generate();

            //Ethio switch does not allow decimal points in amounts, and we have multiplied by 100 according to their request.
            int amount = payment.getAmount().multiply(BigDecimal.valueOf(100)).intValue();

            String requestParameters =
                    "?userName=" + userName +
                            "&password=" + password +
                            "&amount=" + amount +
                            "&orderNumber=" + transRef +
                            "&currency=" + "230" +
                            "&returnUrl=" + returnUrl;

            String urlWithParams = URL_PAYMENT_REQUEST + requestParameters;

            payment.setRequestPayload(requestParameters);
            payment.setTransRef(transRef);
            palPaymentRepository.save(payment);

            RequestBuilder builder = new RequestBuilder("POST");
            builder.setUrl(urlWithParams)
                    .build();

            JSONObject resp = httpProcessor.jsonRequestProcessor(builder);

            if (resp.getString("StatusCode").equals("200")) {
                JSONObject resBody = new JSONObject(resp.getString("ResponseBody"));
                payment.setResponsePayload(resBody.toString());
                payment.setResponseDate(Timestamp.from(Instant.now()));

                if (resBody.optInt("errorCode") == 0) {
                    respBdy.put("statusCode", ResponseCodes.SUCCESS)
                            .put("OrderRef", payment.getOrderRef())
                            .put("TransRef", payment.getTransRef())
                            .put("PaymentUrl", resBody.getString("formUrl"))
                            .put("statusDescription", "Success")
                            .put("statusMessage", "Success");

                    payment.setStatus(1);
                    payment.setBillTransRef(payment.getTransRef());
                    payment.setFinalResponse("0");
                    payment.setFinalResponseMessage("PENDING");
                    payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                    palPaymentRepository.save(payment);
                } else {
                    respBdy.put("statusCode", ResponseCodes.NOT_EXIST)
                            .put("statusDescription", "Request failed")
                            .put("statusMessage", "FAILED")
                            .put("PaymentUrl", "");

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
                        .put("statusMessage", "Request failed")
                        .put("PaymentUrl", "");

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
            System.out.println("==========================================");
            System.out.println(ex.getMessage());
            ex.getStackTrace();
            System.out.println("===========================================");
            log.log(Level.WARNING, ex.getMessage());
            respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed")
                    .put("statusMessage", "Request failed")
                    .put("PaymentUrl", "");
        }
        return respBdy;
    }
}
