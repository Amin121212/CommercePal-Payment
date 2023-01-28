package com.commerce.pal.payment.integ.payment.sahay;

import com.commerce.pal.payment.util.HttpProcessor;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.logging.Level;


@Log
@Service
public class SahayConstants {
    @Value(value = "${org.commerce.pal.sahay.consumer.key}")
    private String consumerKey;
    @Value(value = "${org.commerce.pal.sahay.consumer.secret}")
    private String consumerSecret;

    @Value(value = "${org.commerce.pal.sahay.auth.endpoint}")
    private String URL_AUTH;

    private final HttpProcessor httpProcessor;

    @Autowired
    public SahayConstants(HttpProcessor httpProcessor) {
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

}
