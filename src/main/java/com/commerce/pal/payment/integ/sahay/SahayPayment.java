package com.commerce.pal.payment.integ.sahay;

import com.commerce.pal.payment.util.HttpProcessor;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class SahayPayment {
    @Value(value = "${org.commerce.pal.sahay.consumer.key}")
    private String consumerKey;
    @Value(value = "${org.commerce.pal.sahay.consumer.secret}")
    private String consumerSecret;

    @Value(value = "${org.commerce.pal.sahay.auth.endpoint}")
    private String URL_AUTH;
    @Value(value = "${org.commerce.pal.sahay.payment.request.endpoint}")
    private String URL_PAYMENT_REQUEST;
    @Value(value = "${org.commerce.pal.sahay.payment.fulfillment.endpoint}")
    private String URL_PAYMENT_FULFILLMENT;
    @Value(value = "${org.commerce.pal.sahay.check.customer.endpoint}")
    private String URL_CHECK_CUSTOMER;

    private final HttpProcessor httpProcessor;

    @Autowired
    public SahayPayment(HttpProcessor httpProcessor) {
        this.httpProcessor = httpProcessor;
    }

    public String getToken() {
        try {
            log.log(Level.INFO, "Getting Sahay Bill Payment Access token");

            JSONObject payload = new JSONObject();
            payload.put("consumerKey", consumerKey)
                    .put("consumerSecret", consumerSecret);

            HttpProcessor httpProcessor = new HttpProcessor();
            RequestBuilder builder = new RequestBuilder("POST");
            builder.setUrl(URL_AUTH)
                    .addHeader("Content-Type", "application/json")
                    .setBody(payload.toString())
                    .build();
            String token = httpProcessor.processProperRequest(builder);
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(token);
            } catch (JSONException err) {
                log.log(Level.SEVERE, err.getMessage());
                return null;
            }
            if (token.contains("Error") || token.contains("errorCode")) {
                return null;
            }
            try {
                return jsonObject.getString("AccessToken");
            } catch (JSONException err) {
                log.log(Level.SEVERE, err.getMessage());
            }
            return "Error";
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage());
        }
        return "Error";
    }

    public JSONObject checkCustomer(String phone) {
        JSONObject respBdy = new JSONObject();
        try {
            String accessToken = getToken();
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
