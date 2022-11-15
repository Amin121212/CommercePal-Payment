package com.commerce.pal.payment.util;

import com.commerce.pal.payment.integ.notification.EmailClient;
import com.commerce.pal.payment.integ.notification.SmsService;
import com.commerce.pal.payment.integ.notification.push.OneSignal;
import com.commerce.pal.payment.module.database.AccountService;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

@Log
@Component
public class GlobalMethods {

    @Autowired
    private EmailClient emailClient;
    @Autowired
    private SmsService smsService;


    @Autowired
    private OneSignal oneSignal;
    private final SmsLogging smsLogging;
    private final AccountService accountService;

    @Autowired
    public GlobalMethods(SmsLogging smsLogging, AccountService accountService) {
        this.smsLogging = smsLogging;
        this.accountService = accountService;
    }

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

    public String getAccountBalance(String account) {
        return accountService.getAccountBalance(account);
    }

    public void sendPushNotification(JSONObject payload) {
        oneSignal.pickAndProcess(payload.getString("UserId"),
                payload.getString("Header"),
                payload.getString("Message"),
                payload.getJSONObject("data"));
    }

    public void sendSMS(String message, String phone) {
        smsService.pickAndProcess(message, phone);
    }

    public void sendSMSNotification(JSONObject data) {
        String message = smsLogging.generateMessage(data);
        smsService.pickAndProcess(message, data.getString("Phone"));
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

    public Long getUserId(String type, JSONObject valTokenBdy) {
        Long userId = new Long(0L);
        try {
            if (type.equals("A")) {
                JSONObject agentInfo = valTokenBdy.getJSONObject("agentInfo");
                userId = Long.valueOf(agentInfo.getInt("userId"));
            } else if (type.equals("B")) {
                JSONObject businessInfo = valTokenBdy.getJSONObject("businessInfo");
                userId = Long.valueOf(businessInfo.getInt("userId"));
            } else if (type.equals("M")) {
                JSONObject merchantInfo = valTokenBdy.getJSONObject("merchantInfo");
                userId = Long.valueOf(merchantInfo.getInt("userId"));
            } else if (type.equals("C")) {
                JSONObject userDetails = valTokenBdy.getJSONObject("Details");
                userId = Long.valueOf(userDetails.getInt("userId"));
            }
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
        }
        return userId;
    }
}
