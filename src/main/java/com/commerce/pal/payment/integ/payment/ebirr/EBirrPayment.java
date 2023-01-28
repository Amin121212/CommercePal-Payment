package com.commerce.pal.payment.integ.payment.ebirr;

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
public class EBirrPayment {
    @Value(value = "${org.ebirr.initiate.payment}")
    private String URL_PAYMENT_REQUEST;

    @Value(value = "${org.ebirr.schemaVersion}")
    private String schemaVersion;
    @Value(value = "${org.ebirr.channelName}")
    private String channelName;
    @Value(value = "${org.ebirr.serviceName}")
    private String serviceName;
    @Value(value = "${org.ebirr.merchantUid}")
    private String merchantUid;
    @Value(value = "${org.ebirr.paymentMethod}")
    private String paymentMethod;
    @Value(value = "${org.ebirr.apiKey}")
    private String apiKey;
    @Value(value = "${org.ebirr.apiUserId}")
    private String apiUserId;

    private final HttpProcessor httpProcessor;
    private final PalPaymentRepository palPaymentRepository;

    @Autowired
    public EBirrPayment(HttpProcessor httpProcessor,
                        PalPaymentRepository palPaymentRepository) {
        this.httpProcessor = httpProcessor;
        this.palPaymentRepository = palPaymentRepository;
    }

    public JSONObject pickAndProcess(PalPayment payment) {
        JSONObject respBdy = new JSONObject();
        try {
            JSONObject payload = new JSONObject();
            payload.put("schemaVersion", schemaVersion);
            payload.put("requestId", payment.getTransRef());
            payload.put("timestamp", payment.getTransRef());
            payload.put("channelName", channelName);
            payload.put("serviceName", serviceName);

            JSONObject serviceParams = new JSONObject();
            serviceParams.put("merchantUid", merchantUid);
            serviceParams.put("paymentMethod", paymentMethod);
            serviceParams.put("apiKey", apiKey);
            serviceParams.put("apiUserId", apiUserId);

            String payPhone = "";
            if (payment.getAccountNumber().length() > 9) {
                payPhone = "0" + payment.getAccountNumber().substring(payment.getAccountNumber().length() - 9);
            } else {
                payPhone = "0" + payment.getAccountNumber();
            }
            JSONObject payerInfo = new JSONObject();
            payerInfo.put("accountNo", payPhone);
            serviceParams.put("payerInfo", payerInfo);

            JSONObject transactionInfo = new JSONObject();
            transactionInfo.put("amount", payment.getAmount().toString());
            transactionInfo.put("currency", "ETB");
            transactionInfo.put("description", "Payment for Order:" + payment.getOrderRef());
            transactionInfo.put("referenceId", payment.getTransRef());
            transactionInfo.put("invoiceId", "I" + payment.getTransRef());

            serviceParams.put("transactionInfo", transactionInfo);

            payload.put("serviceParams", serviceParams);

            payment.setRequestPayload(payload.toString());
            palPaymentRepository.save(payment);

            RequestBuilder builder = new RequestBuilder("POST");
            builder.addHeader("Content-Type", "application/json")
                    .setBody(payload.toString())
                    .setUrl(URL_PAYMENT_REQUEST)
                    .build();

            JSONObject resp = httpProcessor.jsonRequestProcessor(builder);

            if (resp.getString("StatusCode").equals("200")) {
                JSONObject resBody = new JSONObject(resp.getString("ResponseBody"));
                payment.setResponsePayload(resBody.toString());
                payment.setResponseDate(Timestamp.from(Instant.now()));
                if (resBody.getString("responseCode").equals("2001")) {
                    JSONObject paramsBdy = resBody.getJSONObject("params");
                    respBdy.put("statusCode", ResponseCodes.SUCCESS)
                            .put("OrderRef", payment.getOrderRef())
                            .put("TransRef", payment.getTransRef())
                            .put("PaymentRef", "issuerTransactionId")
                            .put("statusDescription", "Success")
                            .put("statusMessage", "Success");

                    payment.setBillTransRef(paramsBdy.getString("transactionId"));
                    payment.setStatus(3);
                    payment.setFinalResponse("000");
                    payment.setFinalResponseMessage(resBody.getString("responseMsg") + " - " + paramsBdy.getString("issuerTransactionId"));
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

        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
            respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed")
                    .put("statusMessage", "Request failed");
        }
        return respBdy;
    }
}
