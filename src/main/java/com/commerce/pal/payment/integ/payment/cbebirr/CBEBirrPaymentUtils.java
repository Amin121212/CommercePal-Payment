package com.commerce.pal.payment.integ.payment.cbebirr;

import com.google.common.hash.Hashing;
import lombok.extern.java.Log;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Security;
import java.util.Base64;
import java.util.logging.Level;

@Component
@Log
public class CBEBirrPaymentUtils {

    private static final String SECURITY_KEY = "b14ca5898a4e4133bbce2ea2315a1916";
    private static final String ALGORITHM = "DESede";
    private static final String TRANSFORMATION = "DESede/ECB/PKCS7Padding";

    public String encrypt(String plainText) {
        try {
            Security.insertProviderAt(new BouncyCastleProvider(), 1);

            byte[] plainTextBytes = plainText.getBytes(StandardCharsets.UTF_8);
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] securityKeyHash = md5.digest(SECURITY_KEY.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec tripleDESKey = new SecretKeySpec(securityKeyHash, ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, tripleDESKey);
            byte[] encryptedBytes = cipher.doFinal(plainTextBytes);
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
            throw new RuntimeException("Error Encrypting data: ", ex);
        }
    }

    public String decrypt(String decodedString) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] securityKeyHash = md5.digest(SECURITY_KEY.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec tripleDESKey = new SecretKeySpec(securityKeyHash, ALGORITHM);
            byte[] cipherText = Base64.getDecoder().decode(decodedString.getBytes(StandardCharsets.UTF_8));
            Cipher decryptCipher = Cipher.getInstance(TRANSFORMATION);
            decryptCipher.init(Cipher.DECRYPT_MODE, tripleDESKey);
            byte[] decryptedText = decryptCipher.doFinal(cipherText);
            return new String(decryptedText);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
            throw new RuntimeException("Error Decrypting data: ", ex);
        }
    }

    public String hash(String input) {
        try {
            return Hashing.sha256()
                    .hashString(input, StandardCharsets.UTF_8)
                    .toString();
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
            throw new RuntimeException("Error generating Hash", ex);
        }
    }


}