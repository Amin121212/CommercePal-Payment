package com.commerce.pal.payment.util;

import com.commerce.pal.payment.integ.notification.EmailClient;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Random;

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

    public void processEmailWithTemplate(JSONObject payload) {
        emailClient.emailTemplateSender(payload);
    }

    public void processEmailWithoutTemplate(JSONObject payload) {
        emailClient.emailSender(payload.getString("EmailMessage"),
                payload.getString("EmailDestination"),
                payload.getString("EmailSubject"));
    }

    public String generateValidationCode() {
        Random rnd = new Random();
        Integer n = Integer.valueOf(1000 + rnd.nextInt(9000));
        return n.toString();
    }

    public String encryptCode(String code) {
        return code;
    }

    public String deCryptCode(String code) {
        return code;
    }

    public Integer[] convertListToIntegerArray(List<Integer> list) {
        Integer[] arr = new Integer[list.size()];
        for (int i = 0; i < list.size(); i++)
            arr[i] = list.get(i);
        return arr;
    }
}
