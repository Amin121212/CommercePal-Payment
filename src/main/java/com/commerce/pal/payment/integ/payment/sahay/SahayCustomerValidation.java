package com.commerce.pal.payment.integ.payment.sahay;

import com.commerce.pal.payment.util.HttpProcessor;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class SahayCustomerValidation {


    @Value(value = "${org.commerce.pal.sahay.check.customer.endpoint}")
    private String URL_CHECK_CUSTOMER;

    private final SahayConstants sahayConstants;
    private final HttpProcessor httpProcessor;

    @Autowired
    public SahayCustomerValidation(SahayConstants sahayConstants,
                                   HttpProcessor httpProcessor) {
        this.sahayConstants = sahayConstants;
        this.httpProcessor = httpProcessor;
    }

    public JSONObject checkCustomer(String phone) {
        JSONObject respBdy = new JSONObject();
        try {
            String accessToken = sahayConstants.getToken();
            if (accessToken == null || accessToken.equals("") || accessToken.contains("Error")) {
                log.log(Level.SEVERE, "Unable to get access token");
                respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                        .put("statusDescription", "failed")
                        .put("statusMessage", "Request failed");
            } else {
                JSONObject payload = new JSONObject();
                payload.put("PhoneNumber", phone);

                RequestBuilder builder = new RequestBuilder("POST");
                builder.addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Content-Type", "application/json")
                        .setBody(payload.toString())
                        .setUrl(URL_CHECK_CUSTOMER)
                        .build();

                JSONObject resp = httpProcessor.jsonRequestProcessor(builder);

                if (resp.getString("StatusCode").equals("200")) {
                    JSONObject resBody = new JSONObject(resp.getString("ResponseBody"));
                    if (resBody.getString("response").equals("000")) {
                        respBdy.put("statusCode", ResponseCodes.SUCCESS)
                                .put("statusDescription", "Success")
                                .put("customerName", resBody.getString("customerName"))
                                .put("InsName", "Sahay - Wallet")
                                .put("statusMessage", "Success");
                    } else {
                        respBdy.put("statusCode", ResponseCodes.NOT_EXIST)
                                .put("statusDescription", "failed")
                                .put("statusMessage", "Request failed");
                    }
                } else {
                    respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                            .put("statusDescription", "failed")
                            .put("statusMessage", "Request failed");
                }
            }
        } catch (Exception ex) {
            respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed")
                    .put("statusMessage", "Request failed");
        }
        return respBdy;
    }
}
