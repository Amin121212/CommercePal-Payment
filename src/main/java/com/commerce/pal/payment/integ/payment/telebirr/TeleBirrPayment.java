package com.commerce.pal.payment.integ.payment.telebirr;

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
import java.util.UUID;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class TeleBirrPayment {

    @Value(value = "${org.telebirr.initiate.payment}")
    private String URL_PAYMENT_REQUEST;
    private final HttpProcessor httpProcessor;
    private final PalPaymentRepository palPaymentRepository;
    private final TeleBirrPaymentUtils teleBirrPaymentUtils;

    @Autowired
    public TeleBirrPayment(HttpProcessor httpProcessor,
                           PalPaymentRepository palPaymentRepository, TeleBirrPaymentUtils teleBirrPaymentUtils) {
        this.httpProcessor = httpProcessor;
        this.palPaymentRepository = palPaymentRepository;
        this.teleBirrPaymentUtils = teleBirrPaymentUtils;
    }

    public JSONObject pickAndProcess(PalPayment payment) {
        JSONObject respBdy = new JSONObject();
        try {
            //Both of them must be the same for sign and ussd generation.
            String timestamp = String.valueOf(System.currentTimeMillis());
            String nonce = UUID.randomUUID().toString().replace("-", "");

            JSONObject payload = new JSONObject();
            payload.put("appid", teleBirrPaymentUtils.getAppId());
            payload.put("sign", teleBirrPaymentUtils.generateSignature(payment, timestamp, nonce));
            payload.put("ussd", teleBirrPaymentUtils.generateUssdParameter(payment, timestamp, nonce));

            //cant save payload as request payload, bcuz it`s too large
            JSONObject reqBodyToSave = new JSONObject();
            reqBodyToSave.put("nonce", nonce)
                    .put("outTradeNo", payment.getTransRef())
                    .put("totalAmount", payment.getAmount());

            payment.setRequestPayload(reqBodyToSave.toString());
            palPaymentRepository.save(payment);

            RequestBuilder builder = new RequestBuilder("POST");
            builder.addHeader("Content-Type", "application/json")
                    .setBody(payload.toString())
                    .setUrl(URL_PAYMENT_REQUEST)
                    .build();

            JSONObject resp = httpProcessor.jsonRequestProcessor(builder);

            if (resp.getString("StatusCode").equals("200")) {

                JSONObject resBody = new JSONObject(resp.getString("ResponseBody"));

                //cant save resBody as response payload, bcuz it`s too large
                JSONObject resBodyToSave = new JSONObject();
                resBodyToSave.put("code", resBody.getInt("code"))
                        .put("data", resBody.optJSONObject("data").optString("toPayUrl"))
                        .put("message", resBody.getString("message"));

                payment.setResponsePayload(resBodyToSave.toString());
                payment.setResponseDate(Timestamp.from(Instant.now()));
                palPaymentRepository.save(payment);

                if (resBody.optInt("code") == 200) {
                    respBdy.put("statusCode", ResponseCodes.SUCCESS)
                            .put("OrderRef", payment.getOrderRef())
                            .put("TransRef", payment.getTransRef())
                            .put("PaymentUrl", resBody.optJSONObject("data").optString("toPayUrl"))
                            .put("statusDescription", resBody.optString("message"))
                            .put("statusMessage", "Success");


                    payment.setStatus(1);
                    payment.setBillTransRef(nonce);
                    payment.setFinalResponse("0");
                    payment.setFinalResponseMessage("PENDING");
                    payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                    palPaymentRepository.save(payment);
                } else {
                    respBdy.put("statusCode", ResponseCodes.NOT_EXIST)
                            .put("PaymentUrl", "")
                            .put("statusDescription", "Request failed")
                            .put("statusMessage", "FAILED");

                    payment.setStatus(5);
                    payment.setBillTransRef("FAILED");
                    payment.setFinalResponse("999");
                    payment.setFinalResponseMessage("FAILED");
                    payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                    palPaymentRepository.save(payment);
                }
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
            log.log(Level.WARNING, ex.getMessage());
            respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("PaymentUrl", "")
                    .put("statusDescription", "Request failed")
                    .put("statusMessage", "FAILED");
        }
        return respBdy;
    }
}
