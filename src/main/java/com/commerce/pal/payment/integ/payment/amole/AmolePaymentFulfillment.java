package com.commerce.pal.payment.integ.payment.amole;

import com.commerce.pal.payment.module.payment.ProcessSuccessPayment;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class AmolePaymentFulfillment {

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
    @Value(value = "${org.amole.payment.action.payment}")
    private String paymentAction;
    @Value(value = "${org.amole.tin}")
    private String tin;

    private final HttpProcessor httpProcessor;
    private final PalPaymentRepository palPaymentRepository;
    private final ProcessSuccessPayment processSuccessPayment;

    @Autowired
    public AmolePaymentFulfillment(HttpProcessor httpProcessor,
                                   PalPaymentRepository palPaymentRepository, ProcessSuccessPayment processSuccessPayment) {
        this.httpProcessor = httpProcessor;
        this.palPaymentRepository = palPaymentRepository;
        this.processSuccessPayment = processSuccessPayment;
    }

    public JSONObject pickAndProcess(JSONObject reqBdy) {
        JSONObject respBdy = new JSONObject();
        try {
            palPaymentRepository.findPalPaymentByTransRefAndStatus(reqBdy.getString("TransRef"), 1)
                    .ifPresentOrElse(payment -> {

                        String encodedData = "BODY_CardNumber=" + payment.getAccountNumber() +
                                "&BODY_PIN=" + reqBdy.getString("OTP") +
                                "&BODY_PaymentAction=" + paymentAction +
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
                                    .put("HDR_SourceTransID", resBody.get("HDR_SourceTransID"))
                                    .put("BODY_CardNumber", resBody.get("BODY_CardNumber"))
                                    .put("BODY_Amount", resBody.get("BODY_Amount"))
                                    .put("BODY_PaymentAction", resBody.get("BODY_PaymentAction"))
                                    .put("MSG_ErrorCode", resBody.get("MSG_ErrorCode"))
                                    .put("MSG_ShortMessage", resBody.get("MSG_ShortMessage"));

                            payment.setResponsePayload(resBodyToSave.toString());
                            payment.setResponseDate(Timestamp.from(Instant.now()));
                            palPaymentRepository.save(payment);

                            if (resBody.getString("MSG_ErrorCode").equals("00001")) {

                                LocalDate currentDate = LocalDate.now();
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
                                String formattedDate = currentDate.format(formatter);

                                respBdy.put("statusCode", ResponseCodes.SUCCESS)
                                        .put("statusDescription", resBody.getString("MSG_LongMessage"))
                                        .put("statusMessage", "Success")
                                        .put("OrderRef", payment.getOrderRef())
                                        .put("transRef", payment.getTransRef())
                                        .put("amoleTransRef", resBody.getString("HDR_ReferenceNumber"))
                                        .put("amount", payment.getAmount())
                                        .put("phoneNumber", payment.getAccountNumber())
                                        .put("transDate", formattedDate);

                                payment.setStatus(3);
                                payment.setBillTransRef(resBody.getString("HDR_ReferenceNumber"));
                                payment.setFinalResponse("000");
                                payment.setFinalResponseMessage("SUCCESS");
                                payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                                palPaymentRepository.save(payment);

                                // Process Payment
                                processSuccessPayment.pickAndProcess(payment);
                            } else {
                                respBdy.put("statusCode", ResponseCodes.REQUEST_FAILED)
                                        .put("statusDescription", resBody.getString("MSG_LongMessage"))
                                        .put("statusMessage", "Failed");

                                // don't update payment status. give user another chance
                                payment.setFinalResponseMessage(resBody.getString("MSG_LongMessage"));
                                payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                                palPaymentRepository.save(payment);
                            }
                        } else {
                            respBdy.put("statusCode", ResponseCodes.TRANSACTION_FAILED)
                                    .put("statusDescription", "Request failed")
                                    .put("statusMessage", "Failed");

                            payment.setStatus(5);
                            payment.setBillTransRef("FAILED");
                            payment.setResponsePayload("FAILED");
                            payment.setResponseDate(Timestamp.from(Instant.now()));
                            payment.setFinalResponse("999");
                            payment.setFinalResponseMessage("FAILED");
                            payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                            palPaymentRepository.save(payment);
                        }
                    }, () -> respBdy.put("statusCode", ResponseCodes.TRANSACTION_FAILED)
                            .put("statusDescription", "Failed")
                            .put("statusMessage", "Payment not found"));
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
            respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "Request failed")
                    .put("statusMessage", "Failed");
        }
        return respBdy;
    }
}

