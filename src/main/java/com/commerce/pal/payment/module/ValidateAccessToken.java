package com.commerce.pal.payment.module;

import com.commerce.pal.payment.util.HttpProcessor;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Log
@Service
@SuppressWarnings("Duplicates")
public class ValidateAccessToken {
    @Value(value = "${org.commerce.pal.sahay.validate.access.token}")
    private String VALIDATE_TOKEN_URL;

    private final HttpProcessor httpProcessor;

    @Autowired
    public ValidateAccessToken(HttpProcessor httpProcessor) {
        this.httpProcessor = httpProcessor;
    }

    public JSONObject pickAndProcess(JSONObject rqBdy) {
        JSONObject respBdy = new JSONObject();
        try {
            RequestBuilder builder = new RequestBuilder("GET");
            builder.addHeader("Authorization", rqBdy.getString("AccessToken"))
                    .addHeader("Content-Type", "application/json")
                    .setUrl(VALIDATE_TOKEN_URL)
                    .build();

            JSONObject resp = httpProcessor.jsonRequestProcessor(builder);

            if (resp.getString("StatusCode").equals("200")) {
                JSONObject resBody = new JSONObject(resp.getString("ResponseBody"));
                if (resBody.getString("statusCode").equals("000")) {
                    JSONObject userDetails = resBody.getJSONObject("Details");
                    if (rqBdy.getString("UserType").equals("A")) {
                        if (resBody.getString("IsAgent").equals("YES")) {
                            respBdy.put("Status", "00")
                                    .put("Email", userDetails.getString("email"));
                        } else {
                            respBdy.put("Status", "99")
                                    .put("Message", "Invalid Token");
                        }
                    } else {
                        respBdy.put("Status", "00")
                                .put("Email", userDetails.getString("email"));
                    }
                } else {
                    respBdy.put("Status", "99")
                            .put("Message", "Invalid Token");
                }
            } else {
                respBdy.put("Status", "99")
                        .put("Message", "Invalid Token");
            }
        } catch (Exception ex) {
            respBdy.put("Status", "99")
                    .put("Message", "Invalid Token");
        }
        return respBdy;
    }
}
