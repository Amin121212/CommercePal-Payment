package com.commerce.pal.payment.integ.payment.telebirr;

import com.commerce.pal.payment.module.payment.ProcessSuccessPayment;
import com.commerce.pal.payment.repo.payment.PalPaymentRepository;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class TeleBirrPaymentFulfillment {

    private final PalPaymentRepository palPaymentRepository;
    private final ProcessSuccessPayment processSuccessPayment;
    private final TeleBirrPaymentUtils teleBirrPaymentUtils;

    @Autowired
    public TeleBirrPaymentFulfillment(PalPaymentRepository palPaymentRepository, ProcessSuccessPayment processSuccessPayment, TeleBirrPaymentUtils teleBirrPaymentUtils) {
        this.palPaymentRepository = palPaymentRepository;
        this.processSuccessPayment = processSuccessPayment;
        this.teleBirrPaymentUtils = teleBirrPaymentUtils;
    }

    public JSONObject pickAndProcess(String encryptedReqBdy) {
        JSONObject respBdy = new JSONObject();
        try {
            String decryptedReqBdy = teleBirrPaymentUtils.decryptWithRSA(encryptedReqBdy);
            JSONObject reqBdy = new JSONObject(decryptedReqBdy);

            palPaymentRepository.findPalPaymentByTransRefAndStatus(reqBdy.getString("outTradeNo"), 1)
                    .ifPresentOrElse(payment -> {
                        payment.setResponsePayload(reqBdy.toString());
                        payment.setResponseDate(Timestamp.from(Instant.now()));

                        if (reqBdy.optInt("tradeStatus") == 2) {
                            payment.setStatus(3);
                            payment.setBillTransRef(reqBdy.getString("tradeNo"));
                            payment.setFinalResponse("000");
                            payment.setFinalResponseMessage("SUCCESS");
                            payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                            palPaymentRepository.save(payment);

                            respBdy.put("code", 0)
                                    .put("msg", "success");

                            // Process Payment
                            processSuccessPayment.pickAndProcess(payment);
                        } else if (reqBdy.optInt("tradeStatus") == 1) {
                            payment.setFinalResponseMessage("PENDING");
                            payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                            palPaymentRepository.save(payment);

                            respBdy.put("code", 0)
                                    .put("msg", "Success");
                        } else {
                            payment.setStatus(5);
                            payment.setBillTransRef("FAILED");
                            payment.setFinalResponse("999");
                            payment.setFinalResponseMessage("FAILED");
                            payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                            palPaymentRepository.save(payment);

                            respBdy.put("code", 5)
                                    .put("msg", "Failed");
                        }
                    }, () -> respBdy.put("code", 5)
                            .put("msg", "Failed"));
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
            respBdy.put("code", 5)
                    .put("msg", "Failed");

        }
        return respBdy;
    }
}



