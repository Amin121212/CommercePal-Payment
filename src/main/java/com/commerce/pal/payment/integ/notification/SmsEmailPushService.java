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
public class SmsEmailPushService {

    @Value("${commerce.pal.notification.sms.notification.endpoint}")
    private String SMS_END_POINT;

    @Value("${commerce.pal.notification.email.notification.endpoint}")
    private String EMAIL_END_POINT;

    @Value("${commerce.pal.notification.one.signal.endpoint}")
    private String PUSH_END_POINT;

    private final HttpProcessor httpProcessor;

    @Autowired
    public SmsEmailPushService(HttpProcessor httpProcessor) {
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
                    .setUrl(SMS_END_POINT)
                    .build();
            log.log(Level.INFO, "CommercePal SMS Notification Res : " + httpProcessor.processProperRequest(builder));
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
        }
    }

    public void pickAndProcessEmail(JSONObject emailBody) {
        try {
            RequestBuilder builder = new RequestBuilder("POST");
            builder.addHeader("Content-Type", "application/json")
                    .setBody(emailBody.toString())
                    .setUrl(EMAIL_END_POINT)
                    .build();
            log.log(Level.INFO, "CommercePal Email Notification Res : " + httpProcessor.processProperRequest(builder));
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
        }
    }
    public void pickAndProcessPush(String userId, String header, String message, JSONObject data) {
        try {
            JSONObject pushBdy = new JSONObject();
            pushBdy.put("UserId", userId);
            pushBdy.put("Heading", header);
            pushBdy.put("Message", message);
            pushBdy.put("data", data);
            RequestBuilder builder = new RequestBuilder("POST");
            builder.addHeader("Content-Type", "application/json")
                    .setBody(pushBdy.toString())
                    .setUrl(PUSH_END_POINT)
                    .build();
            log.log(Level.INFO, "CommercePal OneSignal Res : " + httpProcessor.processProperRequest(builder));
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
        }
    }
}
