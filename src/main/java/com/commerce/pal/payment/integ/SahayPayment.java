package com.commerce.pal.payment.integ;

import com.commerce.pal.payment.util.HttpProcessor;
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

    @Value(value = "${org.commerce.pal.sahay.payment.endpoint}")
    private String URL_AUTH;

    @Value(value = "${org.app.properties.gateway.mpesa.gateway.b2c_queue_timeOutURL}")
    private String URL_PAYMENT;


    private final HttpProcessor httpProcessor;

    @Autowired
    public SahayPayment(HttpProcessor httpProcessor) {
        this.httpProcessor = httpProcessor;
    }

    public String getToken() {
        try {
            log.log(Level.INFO, "Getting Mpesa Access token");

            String app_key = consumerKey;
            String app_secret = consumerSecret;
            String appKeySecret = app_key + ":" + app_secret;
            byte[] bytes = appKeySecret.getBytes("ISO-8859-1");
            String encoded = Base64.getEncoder().encodeToString(bytes);

            HttpProcessor httpProcessor = new HttpProcessor();
            RequestBuilder builder = new RequestBuilder("GET");
            builder.addHeader("Authorization",
                            "Basic " + encoded)
                    .setUrl(URL_AUTH)
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
                return jsonObject.getString("access_token");
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
