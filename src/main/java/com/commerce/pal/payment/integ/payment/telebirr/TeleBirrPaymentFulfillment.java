package com.commerce.pal.payment.integ.payment.telebirr;

import com.commerce.pal.payment.integ.payment.hellocash.HelloCashConstants;
import com.commerce.pal.payment.module.payment.ProcessSuccessPayment;
import com.commerce.pal.payment.repo.payment.PalPaymentRepository;
import com.commerce.pal.payment.util.HttpProcessor;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    public TeleBirrPaymentFulfillment(PalPaymentRepository palPaymentRepository,
                                      ProcessSuccessPayment processSuccessPayment) {
        this.palPaymentRepository = palPaymentRepository;
        this.processSuccessPayment = processSuccessPayment;
    }

    public JSONObject pickAndProcess(JSONObject reqBdy) {
        JSONObject respBdy = new JSONObject();
        try {
            palPaymentRepository.findPalPaymentByTransRefAndStatus(
                    reqBdy.getString("transRef"), 1
            ).ifPresentOrElse(payment -> {
                payment.setResponsePayload(reqBdy.toString());
                payment.setResponseDate(Timestamp.from(Instant.now()));
                if (reqBdy.getString("statusCode").equals("000")) {
                    payment.setStatus(3);
                    payment.setBillTransRef(reqBdy.getString("outTradeNo"));
                    payment.setFinalResponse("000");
                    payment.setFinalResponseMessage(reqBdy.getString("statusDescription"));
                    payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                    palPaymentRepository.save(payment);
                    respBdy.put("statusCode", ResponseCodes.SUCCESS)
                            .put("billRef", payment.getOrderRef())
                            .put("TransRef", payment.getTransRef())
                            .put("statusDescription", "Success")
                            .put("statusMessage", "Success");

                    // Process Payment
                    processSuccessPayment.pickAndProcess(payment);
                } else {
                    payment.setStatus(5);
                    payment.setBillTransRef("FAILED");
                    payment.setFinalResponse("999");
                    payment.setFinalResponseMessage(reqBdy.getString("statusDescription"));
                    payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                    palPaymentRepository.save(payment);

                    respBdy.put("statusCode", ResponseCodes.TRANSACTION_FAILED)
                            .put("statusDescription", "failed")
                            .put("statusMessage", "Request failed");
                }
            }, () -> {
                respBdy.put("statusCode", ResponseCodes.TRANSACTION_FAILED)
                        .put("statusDescription", "failed")
                        .put("statusMessage", "Request failed");
            });
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
            respBdy.put("statusCode", ResponseCodes.TRANSACTION_FAILED)
                    .put("statusDescription", "failed")
                    .put("statusMessage", ex.getMessage());
        }
        return respBdy;
    }
}
