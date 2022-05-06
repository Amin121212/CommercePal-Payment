package com.commerce.pal.payment.util;

import com.commerce.pal.payment.integ.notification.EmailClient;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

@Log
@Component
public class GlobalMethods {
    @Autowired
    private EmailClient emailClient;

    public String generateTrans() {
        String ref = Timestamp.from(Instant.now()).toString();
        ref = IDGenerator.getInstance("SB").getRRN();
        return ref;
    }

    @Async
    public void processEmailWithTemplate(JSONObject payload) {
        emailClient.emailTemplateSender(payload);
    }
}
