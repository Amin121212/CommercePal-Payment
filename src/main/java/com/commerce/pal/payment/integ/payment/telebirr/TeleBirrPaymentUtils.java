package com.commerce.pal.payment.integ.payment.telebirr;

import com.commerce.pal.payment.model.payment.PalPayment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.logging.Level;

@Component
@Log
public class TeleBirrPaymentUtils {

    private static final String TELEBIRR_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnzOJHJmpkj0Q4ejjegDnj5rSRqPXsnxYCtMmRcEygY3ooTsv9ng6HGYgfZn805//Y19NQoVl0lww04wO5mXMDj9Ahr1I3zQ+KZFcAYxmyWa7537W4rCr1o8EEMFdJ01hp7opkkK9sGZCrUu7xDRVtDwS81uPb8MESAyFATc8H5JUp3YMj87a2XR36benFpCNE/u+INDLkCm9zVqlVcUGJyULYqLj3sCDH7dxtmLS/IbOImYX8eyAangvbY4nLtWtR7Pww18oRbcXg0ckZMqVZujMoHCWWgJYz56iUmRvZYFS+YwUzQ5ZuqAgxarKDTLIM8SaR4nU2g8TNXWSkSZxtwIDAQAB";
    private static final int CHUNK_SIZE = 256; // Maximum block size for 2048-bit RSA key

    @Value(value = "${org.telebirr.appId}")
    private String appId;
    @Value(value = "${org.telebirr.appKey}")
    private String appKey;
    @Value(value = "${org.telebirr.shortCode}")
    private String shortCode;
    @Value(value = "${org.telebirr.returnUrl}")
    private String returnUrl;
    @Value(value = "${org.telebirr.notifyUrl}")
    private String notifyUrl;

    public String getAppId() {
        return appId;
    }

    private String returnAppValue() throws JsonProcessingException {
        Map<String, String> returnAppMap = new LinkedHashMap<>();
        returnAppMap.put("PackageName", "cn.tydic.ethiopay");
        returnAppMap.put("Activity", "cn.tydic.ethiopay.PayForOtherAppActivity");

        // Convert the returnApp Map to a JSON string
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValueAsString(returnAppMap);

        return "test";
    }

    public String generateSignature(PalPayment payment, String timestamp, String nonce) {
        try {
            String stringA = "appId=" + appId +
                    "&appKey=" + appKey +
                    "&nonce=" + nonce +
                    "&notifyUrl=" + notifyUrl +
                    "&outTradeNo=" + payment.getTransRef() +
                    "&receiveName=" + "HudHud Express" +
                    "&returnUrl=" + returnUrl +
                    "&shortCode=" + shortCode +
                    "&subject=" + "Products" +
                    "&timeoutExpress=" + "30" +
                    "&timestamp=" + timestamp +
                    "&totalAmount=" + payment.getAmount();

            // Perform SHA-256 on stringA
            return Hashing.sha256()
                    .hashString(stringA, StandardCharsets.UTF_8)
                    .toString();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage());
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public String generateSignatureForMobile(PalPayment payment, String timestamp, String nonce) {
        try {
            String stringA = "appId=" + appId +
                    "&appKey=" + appKey +
                    "&nonce=" + nonce +
                    "&notifyUrl=" + notifyUrl +
                    "&outTradeNo=" + payment.getTransRef() +
                    "&receiveName=" + "HudHud Express" +
                    "&returnApp=" + returnAppValue() +
                    "&shortCode=" + shortCode +
                    "&subject=" + "Products" +
                    "&timeoutExpress=" + "30" +
                    "&timestamp=" + timestamp +
                    "&totalAmount=" + payment.getAmount();

            // Perform SHA-256 on stringA
            return Hashing.sha256()
                    .hashString(stringA, StandardCharsets.UTF_8)
                    .toString();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage());
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }


    public String generateUssdParameter(PalPayment payment, String timestamp, String nonce) {
        try {
            // Step 1: Convert parameters to JSON string
            String jsonString = generateDynamicUssdParameters(payment, timestamp, nonce);

            // Step 2: Perform RSA2048 encryption
            return encryptWithRSA(jsonString);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage());
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public String generateUssdParameterForMobile(PalPayment payment, String timestamp, String nonce) {
        try {
            // Step 1: Convert parameters to JSON string
            String jsonString = generateDynamicUssdParametersForMobilePay(payment, timestamp, nonce);

            // Step 2: Perform RSA2048 encryption
            return encryptWithRSA(jsonString);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage());
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private String generateDynamicUssdParameters(PalPayment payment, String timestamp, String nonce) {
        try {
            Map<String, String> ussdParameters = new LinkedHashMap<>();

            ussdParameters.put("appId", appId);
            ussdParameters.put("nonce", nonce);
            ussdParameters.put("notifyUrl", notifyUrl);
            ussdParameters.put("outTradeNo", payment.getTransRef());
            ussdParameters.put("receiveName", "HudHud Express");
            ussdParameters.put("returnUrl", returnUrl);
            ussdParameters.put("shortCode", shortCode);
            ussdParameters.put("subject", "Products");
            ussdParameters.put("timeoutExpress", "30");
            ussdParameters.put("timestamp", timestamp);
            ussdParameters.put("totalAmount", String.valueOf(payment.getAmount()));

            // Serialize the ussdParameters map to JSON
            return new ObjectMapper().writeValueAsString(ussdParameters);
        } catch (JsonProcessingException ex) {
            log.log(Level.SEVERE, ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    private String generateDynamicUssdParametersForMobilePay(PalPayment payment, String timestamp, String nonce) {
        try {
            Map<String, String> ussdParameters = new LinkedHashMap<>();

            // Populate the Map with the provided data
            ussdParameters.put("appId", appId);
            ussdParameters.put("nonce", nonce);
            ussdParameters.put("notifyUrl", notifyUrl);
            ussdParameters.put("outTradeNo", payment.getTransRef());
            ussdParameters.put("receiveName", "HudHud Express");

            ussdParameters.put("returnApp", returnAppValue());
            ussdParameters.put("shortCode", shortCode);
            ussdParameters.put("subject", "Products");
            ussdParameters.put("timeoutExpress", "30");
            ussdParameters.put("timestamp", timestamp);
            ussdParameters.put("totalAmount", String.valueOf(payment.getAmount()));

            // Serialize the ussdParameters map to JSON
            return new ObjectMapper().writeValueAsString(ussdParameters);
        } catch (JsonProcessingException ex) {
            log.log(Level.SEVERE, ex.getMessage());
            throw new RuntimeException(ex);
        }

    }

    public String encryptWithRSA(String jsonString) {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(TELEBIRR_PUBLIC_KEY);

            // Create a PublicKey object from the public key bytes
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            // Encrypted data
            byte[] encryptedData = encrypt(jsonString.getBytes(), publicKey);
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage());
            throw new RuntimeException(ex.getMessage());
        }
    }


    //Now this encryption is based on hybrid method
    //Note that the RSA algo has limited key size and can't
    //encrypt data more than 117 bytes
    private static byte[] encrypt(byte[] data, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            int blockSize = getBlockSize(publicKey);
            int length = data.length;
            int blocks = (int) Math.ceil((double) length / (blockSize - 11)); // Consider padding size
            byte[] encryptedData = new byte[0];
            for (int i = 0; i < blocks; i++) {
                int start = i * (blockSize - 11);
                int end = Math.min(start + (blockSize - 11), length);
                byte[] chunk = new byte[end - start];
                System.arraycopy(data, start, chunk, 0, chunk.length);
                byte[] encryptedChunk = cipher.doFinal(chunk);
                encryptedData = concatenate(encryptedData, encryptedChunk);
            }
            return encryptedData;

        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage());
            throw new RuntimeException(ex.getMessage());
        }
    }

    // Step 4: Combine the encrypted chunks
    private static byte[] concatenate(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private static int getBlockSize(Key key) {
        return ((RSAKey) key).getModulus().bitLength() / 8;
    }


    // for decryption
    public String decryptWithRSA(String encryptedData) {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(TELEBIRR_PUBLIC_KEY);

            // Get public key
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            // Decrypt the data by splitting it into chunks
            List<byte[]> decryptedChunks = decryptInChunks(encryptedData, TELEBIRR_PUBLIC_KEY);

            // Combine decrypted chunks into a single byte array
            byte[] decryptedData = new byte[decryptedChunks.size() * CHUNK_SIZE];
            int offset = 0;
            for (byte[] decryptedChunk : decryptedChunks) {
                System.arraycopy(decryptedChunk, 0, decryptedData, offset, decryptedChunk.length);
                offset += decryptedChunk.length;
            }

            return new String(decryptedData, StandardCharsets.UTF_8).trim();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage());
            throw new RuntimeException(ex.getMessage());
        }
    }

    private static List<byte[]> decryptInChunks(String encryptedData, String publicKeyString) throws Exception {
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);

        List<byte[]> decryptedChunks = new ArrayList<>();
        for (int offset = 0; offset < encryptedBytes.length; offset += CHUNK_SIZE) {
            int chunkLength = Math.min(CHUNK_SIZE, encryptedBytes.length - offset);
            byte[] chunk = new byte[chunkLength];
            System.arraycopy(encryptedBytes, offset, chunk, 0, chunkLength);

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, getPublicKey(publicKeyString));
            byte[] decryptedChunk = cipher.doFinal(chunk);

            decryptedChunks.add(decryptedChunk);
        }

        return decryptedChunks;
    }

    private static PublicKey getPublicKey(String publicKeyString) throws Exception {
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
    }
}
