package com.commerce.pal.payment.integ.payment.cbebirr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class CBEBirrPaymentService {

    @Value(value = "${org.cbe.birr.authenticate.userID}")
    private String userID;
    @Value(value = "${org.cbe.birr.authenticate.credentials}")
    private String password;
    @Value(value = "${org.cbe.birr.authenticate.merchantCode}")
    private String merchantCode;
    private static final String SECURITY_KEY = "b14ca5898a4e4133bbce2ea2315a1916";

    private final CBEBirrPaymentUtils cbeBirrPaymentUtils;

    public CBEBirrPaymentService(CBEBirrPaymentUtils cbeBirrPaymentUtils) {
        this.cbeBirrPaymentUtils = cbeBirrPaymentUtils;
    }

    public String generatePaymentUrl(String amount, String transactionId) {

        String formattedJsonForHash = generateFormattedJsonForHash(userID, password, transactionId, amount, merchantCode);
        String hashValue = cbeBirrPaymentUtils.hash(formattedJsonForHash);

        String encryptedUserID = cbeBirrPaymentUtils.encrypt(userID);
        String encryptedPassword = cbeBirrPaymentUtils.encrypt(password);
        String encryptedTransactionId = cbeBirrPaymentUtils.encrypt(transactionId);
        String encryptedAmount = cbeBirrPaymentUtils.encrypt(amount);
        String encryptedMerchantCode = cbeBirrPaymentUtils.encrypt(merchantCode);
        String encryptedHashValue = cbeBirrPaymentUtils.encrypt(hashValue);

        String formattedJsonForEncryption = generateEncryptedRequestBody(
                encryptedUserID,
                encryptedPassword,
                encryptedTransactionId,
                encryptedAmount,
                encryptedMerchantCode,
                encryptedHashValue);

        return cbeBirrPaymentUtils.encrypt(formattedJsonForEncryption);
    }

    private String generateFormattedJsonForHash(
            String userID, String password, String transactionId, String amount, String merchantCode) {

        TreeMap<String, String> payload = new TreeMap<>();
        payload.put("U", userID);
        payload.put("W", password);
        payload.put("T", transactionId);
        payload.put("A", amount);
        payload.put("MC", merchantCode);
        payload.put("Key", SECURITY_KEY);

        List<String> temp = new ArrayList<>();
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            temp.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
        }
        return String.join("&", temp);
    }

    private String generateEncryptedRequestBody(
            String encryptedUserID,
            String encryptedPassword,
            String encryptedTransactionId,
            String encryptedAmount,
            String encryptedMerchantCode,
            String encryptedHashValue) {
        Map<String, String> body = new LinkedHashMap<>();
        try {
            body.put("U", encryptedUserID);
            body.put("W", encryptedPassword);
            body.put("T", encryptedTransactionId);
            body.put("A", encryptedAmount);
            body.put("MC", encryptedMerchantCode);
            body.put("HV", encryptedHashValue);

            return new ObjectMapper().writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            log.log(Level.WARNING, ex.getMessage());
            throw new RuntimeException(ex.getMessage());
        }
    }
}



