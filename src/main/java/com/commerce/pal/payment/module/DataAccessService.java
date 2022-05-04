package com.commerce.pal.payment.module;

import com.commerce.pal.payment.util.HttpProcessor;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Log
@Service
@SuppressWarnings("Duplicates")
public class DataAccessService {

    @Value(value = "${org.commerce.pal.sahay.data.access.endpoint}")
    private String DATA_ACCESS_ENDPOINT;

    private final HttpProcessor httpProcessor;

    public DataAccessService(HttpProcessor httpProcessor) {
        this.httpProcessor = httpProcessor;
    }

    public JSONObject pickAndProcess(JSONObject rqBdy) {
        JSONObject respBdy = new JSONObject();
        try {
            RequestBuilder builder = new RequestBuilder("POST");
            builder.addHeader("Content-Type", "application/json")
                    .setBody(rqBdy.toString())
                    .setUrl(DATA_ACCESS_ENDPOINT)
                    .build();

            JSONObject resp = httpProcessor.jsonRequestProcessor(builder);

            if (resp.getString("StatusCode").equals("200")) {
                respBdy = new JSONObject(resp.getString("ResponseBody"));
                respBdy.put("Status", "00");
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
