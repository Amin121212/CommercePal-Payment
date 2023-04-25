package com.commerce.pal.payment.util;


import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.PersistenceContext;
import javax.persistence.StoredProcedureQuery;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class SmsLogging {

    @PersistenceContext
    private EntityManager entityManager;

    private final HttpProcessor httpProcessor;

    @Autowired
    public SmsLogging(HttpProcessor httpProcessor) {
        this.httpProcessor = httpProcessor;
    }

    public String generateMessage(JSONObject data) {
        JSONObject payResponse = pickAndProcess(data);
        AtomicReference<String> message = new AtomicReference<>(payResponse.getString("Template"));
        try {
            Iterator<String> keys = data.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    message.set(message.get().replace("[" + key + "?]", data.getString(key)));
                } catch (Exception ex) {
                    log.log(Level.WARNING, "Error Mapping SMS : " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.log(Level.WARNING, "Error Generating SMS : " + ex.getMessage());
        }
        return message.get();
    }

    public JSONObject pickAndProcess(JSONObject data) {
        JSONObject transResponse = new JSONObject();
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("GetNotificationTemplate");
            query.registerStoredProcedureParameter("TemplateId", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("TemplateLanguage", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("Template", String.class, ParameterMode.OUT);
            query.setParameter("TemplateId", data.getString("TemplateId"));
            query.setParameter("TemplateLanguage", data.getString("TemplateLanguage"));
            query.execute();
            transResponse.put("Template", query.getOutputParameterValue("Template"));
            transResponse.put("Status", "00");
            transResponse.put("Message", "The request was processed successfully");
        } catch (Exception ex) {
            log.log(Level.WARNING, "SMS TEMPLATE CLASS : " + ex.getMessage());
            transResponse.put("Status", "101");
            transResponse.put("Message", "Failed while processing the request");
            transResponse.put("Template", "Welcome to CommercePal");
        }
        return transResponse;
    }
}
