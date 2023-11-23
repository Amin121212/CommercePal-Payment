package com.commerce.pal.payment.integ.payment.cbebirr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
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


    public String generatePaymentUrl(String amount, String transactionId) {

        String formattedJsonForHash = generateFormattedJsonForHash(userID, password, transactionId, amount, merchantCode);
        String hashValue = CBEBirrUtils.hash(formattedJsonForHash);

        String encryptedUserID = CBEBirrUtils.encrypt(userID);
        String encryptedPassword = CBEBirrUtils.encrypt(password);
        String encryptedTransactionId = CBEBirrUtils.encrypt(transactionId);
        String encryptedAmount = CBEBirrUtils.encrypt(amount);
        String encryptedMerchantCode = CBEBirrUtils.encrypt(merchantCode);
        String encryptedHashValue = CBEBirrUtils.encrypt(hashValue);

        String formattedJsonForEncryption = generateEncryptedRequestBody(
                encryptedUserID,
                encryptedPassword,
                encryptedTransactionId,
                encryptedAmount,
                encryptedMerchantCode,
                encryptedHashValue);

        return CBEBirrUtils.encrypt(formattedJsonForEncryption);
    }

    private String generateFormattedJsonForHash(
            String userID, String password, String transactionId, String amount, String merchantCode) {
        Map<String, String> body = new LinkedHashMap<>();
        try {
            body.put("UserID", userID);
            body.put("Password", password);
            body.put("TransactionId", transactionId);
            body.put("Amount", amount);
            body.put("MerchantCode", merchantCode);

            return new ObjectMapper().writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            log.log(Level.WARNING, ex.getMessage());
            throw new RuntimeException(ex.getMessage());
        }
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



