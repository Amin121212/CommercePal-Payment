package com.commerce.pal.payment.integ.payment.hellocash;

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
@SuppressWarnings("Duplicates")
public class Constants {

    @Value(value = "${org.hello.cash.authenticate}")
    private String URL_AUTH;

    @Value(value = "${org.hello.cash.authenticate.principal}")
    private String principal;
    @Value(value = "${org.hello.cash.authenticate.credentials}")
    private String credentials;
    @Value(value = "${org.hello.cash.authenticate.system}")
    private String system;

    private final HttpProcessor httpProcessor;

    @Autowired
    public Constants(HttpProcessor httpProcessor) {
        this.httpProcessor = httpProcessor;
    }

    public String getToken() {
        try {
            log.log(Level.INFO, "Getting Hello Cash Bill Payment Access token");

            JSONObject payload = new JSONObject();
            payload.put("principal", principal)
                    .put("credentials", credentials)
                    .put("token", "")
                    .put("system", system);

            RequestBuilder builder = new RequestBuilder("POST");

            builder.setUrl(URL_AUTH)
                    .addHeader("Content-Type", "application/json")
                    .setBody(payload.toString())
                    .build();
            String token = httpProcessor.processProperRequest(builder);
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(token);
                if (token.contains("token")) {
                    return jsonObject.getString("token");
                } else {
                    return "Error";
                }
            } catch (JSONException err) {
                log.log(Level.SEVERE, err.getMessage());
                return "Error";
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage());
        }
        return "Error";
    }

}
