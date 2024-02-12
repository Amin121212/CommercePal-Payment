package com.commerce.pal.payment.integ.payment.amole;

import com.commerce.pal.payment.model.payment.PalPayment;
import com.commerce.pal.payment.repo.payment.PalPaymentRepository;
import com.commerce.pal.payment.util.HttpProcessor;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class AmolePayment {
    @Value(value = "${org.amole.payment.url}")
    private String URL_PAYMENT_REQUEST;
    @Value(value = "${org.amole.signature}")
    private String signature;
    @Value(value = "${org.amole.ipAddress}")
    private String ipAddress;
    @Value(value = "${org.amole.authenticate.userName}")
    private String userName;
    @Value(value = "${org.amole.authenticate.password}")
    private String password;
    @Value(value = "${org.amole.authenticate.amoleMerchantID}")
    private String amoleMerchantID;
    @Value(value = "${org.amole.payment.action.authorization}")
    private String authorizationAction;
    @Value(value = "${org.amole.tin}")
    private String tin;


    private final HttpProcessor httpProcessor;
    private final PalPaymentRepository palPaymentRepository;

    @Autowired
    public AmolePayment(HttpProcessor httpProcessor,
                        PalPaymentRepository palPaymentRepository) {
        this.httpProcessor = httpProcessor;
        this.palPaymentRepository = palPaymentRepository;
    }

    public JSONObject pickAndProcess(PalPayment payment) {
        JSONObject respBdy = new JSONObject();
        try {
            String encodedData = "BODY_CardNumber=" + payment.getAccountNumber() +
                    "&BODY_PaymentAction=" + authorizationAction +
                    "&BODY_AmountX=" + payment.getAmount() +
                    "&BODY_AmoleMerchantID=" + amoleMerchantID +
                    "&BODY_OrderDescription=" + "payment for commercepal.com" +
                    "&BODY_SourceTransID=" + payment.getTransRef() +
                    "&BODY_VendorAccount=" + tin;

            payment.setRequestPayload(encodedData);
            palPaymentRepository.save(payment);

            RequestBuilder builder = new RequestBuilder("POST");
            builder.setUrl(URL_PAYMENT_REQUEST)
                    .addHeader("HDR_Signature", signature)
                    .addHeader("HDR_IPAddress", ipAddress)
                    .addHeader("HDR_UserName", userName)
                    .addHeader("HDR_Password", password)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .setBody(new ByteArrayInputStream(encodedData.getBytes()))
                    .build();


            JSONObject resp = httpProcessor.jsonRequestProcessor(builder);

            if (resp.getString("StatusCode").equals("200")) {
                JSONArray responseBodyArray = new JSONArray(resp.getString("ResponseBody"));
                JSONObject resBody = responseBodyArray.getJSONObject(0);

                //cant save payload as request payload, bcuz it`s too large
                JSONObject resBodyToSave = new JSONObject();
                resBodyToSave
                        .put("HDR_ResponseID", resBody.get("HDR_ResponseID"))
                        .put("HDR_Acknowledge", resBody.get("HDR_Acknowledge"))
                        .put("HDR_SourceTransID", resBody.get("HDR_SourceTransID"))
                        .put("BODY_AuthorizationCode", resBody.get("BODY_AuthorizationCode"))
                        .put("BODY_CardNumber", resBody.get("BODY_CardNumber"))
                        .put("BODY_Amount", resBody.get("BODY_Amount"))
                        .put("BODY_PaymentAction", resBody.get("BODY_PaymentAction"))
                        .put("MSG_ErrorCode", resBody.get("MSG_ErrorCode"))
                        .put("MSG_ShortMessage", resBody.get("MSG_ShortMessage"));

                payment.setResponsePayload(resBodyToSave.toString());
                payment.setResponseDate(Timestamp.from(Instant.now()));

                if (resBody.getString("MSG_ErrorCode").equals("00001")) {
                    respBdy.put("statusCode", ResponseCodes.SUCCESS)
                            .put("OrderRef", payment.getOrderRef())
                            .put("TransRef", payment.getTransRef())
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
                            .put("statusMessage", "Failed 3");


                    respBdy.put("statusCode", ResponseCodes.REQUEST_FAILED)
                            .put("statusDescription", resBody.getString("MSG_LongMessage"))
                            .put("statusMessage", "Failed");

                    // don't update payment status. give user another chance
                    payment.setFinalResponseMessage(resBody.getString("MSG_LongMessage"));
                    payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                    palPaymentRepository.save(payment);
                }
            } else {
                respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                        .put("statusDescription", "Request failed")
                        .put("statusMessage", "Failed 2");

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
            ex.getStackTrace();
            log.log(Level.WARNING, ex.getMessage());
            respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "Request failed")
                    .put("statusMessage", "Failed 1");
        }
        return respBdy;
    }
}
