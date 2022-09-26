package com.commerce.pal.payment.module.database;

import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.PersistenceContext;
import javax.persistence.StoredProcedureQuery;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class PaymentStoreProcedure {
    @PersistenceContext
    private EntityManager entityManager;

    public JSONObject processOrderPayment(JSONObject reqBody) {
        JSONObject transResponse = new JSONObject();
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("ExecuteOrderPayment");
            query.registerStoredProcedureParameter("TransRef", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("PaymentType", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("PaymentAccountType", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("CountryCode", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("Currency", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("TotalAmount", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("OrderPayment", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("TaxAmount", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("DeliveryAmount", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("PaymentNarration", String.class, ParameterMode.IN);

            query.setParameter("TransRef", reqBody.getString("TransRef"));
            query.setParameter("PaymentType", reqBody.getString("PaymentType"));
            query.setParameter("PaymentAccountType", reqBody.getString("PaymentAccountType"));
            query.setParameter("CountryCode", reqBody.getString("CountryCode"));
            query.setParameter("Currency", reqBody.getString("Currency"));
            query.setParameter("TotalAmount", reqBody.getString("TotalAmount"));
            query.setParameter("OrderPayment", reqBody.getString("OrderPayment"));
            query.setParameter("TaxAmount", reqBody.getString("TaxAmount"));
            query.setParameter("DeliveryAmount", reqBody.getString("DeliveryAmount"));
            query.setParameter("PaymentNarration", reqBody.getString("PaymentNarration"));

             /*
            OUTPUT PARAMS
             */
            query.registerStoredProcedureParameter("TransactionStatus", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("Narration", String.class, ParameterMode.OUT);

            query.execute();

            transResponse.put("TransactionStatus", query.getOutputParameterValue("TransactionStatus"));
            transResponse.put("Narration", query.getOutputParameterValue("Narration"));
            transResponse.put("Status", "00");
            transResponse.put("Message", "The request was processed successfully");
        } catch (Exception ex) {
            log.log(Level.WARNING, "processOrderPayment CLASS : " + ex.getMessage());
            transResponse.put("Status", "101");
            transResponse.put("Message", "Failed while processing the request");
        }
        return transResponse;

    }

    public JSONObject agentCashPayment(JSONObject reqBody) {
        JSONObject transResponse = new JSONObject();
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("ExecuteAgentCashPayment");
            query.registerStoredProcedureParameter("TransRef", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("AgentEmail", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("Currency", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("Amount", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("PaymentNarration", String.class, ParameterMode.IN);

            query.setParameter("TransRef", reqBody.getString("TransRef"));
            query.setParameter("AgentEmail", reqBody.getString("UserEmail"));
            query.setParameter("Currency", reqBody.getString("Currency"));
            query.setParameter("Amount", reqBody.getString("Amount"));
            query.setParameter("PaymentNarration", reqBody.getString("PaymentNarration"));


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
            log.log(Level.WARNING, "processOrderPayment CLASS : " + ex.getMessage());
            transResponse.put("Status", "101");
            transResponse.put("Message", ex.getMessage());
        }
        return transResponse;
    }

    public JSONObject merchantItemSettlement(JSONObject reqBody) {
        JSONObject transResponse = new JSONObject();
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("ExecuteMerchantPayment");
            query.registerStoredProcedureParameter("TransRef", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("ItemId", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("PaymentNarration", String.class, ParameterMode.IN);
            query.setParameter("TransRef", reqBody.getString("TransRef"));
            query.setParameter("ItemId", reqBody.getString("ItemId"));
            query.setParameter("PaymentNarration", reqBody.getString("PaymentNarration"));

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
            log.log(Level.WARNING, "Merchant Item Settlement CLASS : " + ex.getMessage());
            transResponse.put("Status", "101");
            transResponse.put("Message", ex.getMessage());
        }
        return transResponse;
    }

    public JSONObject merchantWithdrawal(JSONObject reqBody) {
        JSONObject transResponse = new JSONObject();
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("ExecuteMerchantWithdrawal");
            query.registerStoredProcedureParameter("TransRef", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("MerchantEmail", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("Currency", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("Amount", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("WithdrawalMethod", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("PaymentNarration", String.class, ParameterMode.IN);

            query.setParameter("TransRef", reqBody.getString("TransRef"));
            query.setParameter("MerchantEmail", reqBody.getString("MerchantEmail"));
            query.setParameter("Currency", reqBody.getString("Currency"));
            query.setParameter("Amount", reqBody.getString("Amount"));
            query.setParameter("WithdrawalMethod", reqBody.getString("WithdrawalMethod"));
            query.setParameter("PaymentNarration", reqBody.getString("PaymentNarration"));


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
            log.log(Level.WARNING, "processOrderPayment CLASS : " + ex.getMessage());
            transResponse.put("Status", "101");
            transResponse.put("Message", ex.getMessage());
        }
        return transResponse;
    }
}
