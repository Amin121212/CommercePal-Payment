package com.commerce.pal.payment.integ.notification;

import com.commerce.pal.payment.util.HttpProcessor;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.logging.Level;
@Log
@Service
public class SmsService {

    @Value("${commerce.pal.notification.sms.notification.endpoint}")
    private String PUSH_END_POINT;

    private final HttpProcessor httpProcessor;

    @Autowired
    public SmsService(HttpProcessor httpProcessor) {
        this.httpProcessor = httpProcessor;
    }

    public void pickAndProcess(String message, String phone) {
        try {
            JSONObject pushBdy = new JSONObject();
            pushBdy.put("Phone", phone);
            pushBdy.put("Message", message);
            RequestBuilder builder = new RequestBuilder("POST");
            builder.addHeader("Content-Type", "application/json")
                    .setBody(pushBdy.toString())
                    .setUrl(PUSH_END_POINT)
                    .build();
            log.log(Level.INFO, "CommercePal Notification Res : " + httpProcessor.processProperRequest(builder));
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
        }
    }
}
