package com.commerce.pal.payment.module.database;

import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.PersistenceContext;
import javax.persistence.StoredProcedureQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

@Log
@Service
@SuppressWarnings("Duplicates")
public class AccountService {
    @PersistenceContext
    private EntityManager entityManager;

    public String getAccountBalance(String account) {
        AtomicReference<String> balance = new AtomicReference<>("0.00");
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("SpGetAccountBalance");
            query.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
            query.setParameter(1, account);
            List response = query.getResultList();
            response.forEach(res -> {
                try {
                    List<?> resData = new ArrayList();
                    if (res.getClass().isArray()) {
                        resData = Arrays.asList((Object[]) res);
                        balance.set(resData.get(0).toString());
                    } else if (res instanceof Collection) {
                        balance.set(res.toString());
                    } else {
                        balance.set(res.toString());
                    }
                } catch (Exception ex) {
                    log.log(Level.WARNING, ex.getMessage());
                }
            });
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
        }
        return balance.get();
    }

    public JSONObject processAgentFloat(JSONObject reqBody) {
        JSONObject transResponse = new JSONObject();
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("ExecuteAgentFloat");
            query.registerStoredProcedureParameter("TransRef", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("Account", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("Currency", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("Amount", String.class, ParameterMode.IN);

            query.setParameter("TransRef", reqBody.getString("TransRef"));
            query.setParameter("Account", reqBody.getString("Account"));
            query.setParameter("Currency", reqBody.getString("Currency"));
            query.setParameter("Amount", reqBody.getString("Amount"));
             /*
            OUTPUT PARAMS
             */
            query.registerStoredProcedureParameter("TransactionStatus", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("Balance", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("Narration", String.class, ParameterMode.OUT);

            query.execute();

            transResponse.put("TransactionStatus", query.getOutputParameterValue("TransactionStatus"));
            transResponse.put("Balance", query.getOutputParameterValue("Balance"));
            transResponse.put("Narration", query.getOutputParameterValue("Narration"));
            transResponse.put("Status", "00");
            transResponse.put("Message", "The request was processed successfully");
        } catch (Exception ex) {
            log.log(Level.WARNING, "ProcessAgentFloat CLASS : " + ex.getMessage());
            transResponse.put("Status", "101");
            transResponse.put("Message", "Failed while processing the request");
        }
        return transResponse;
    }
}
