package com.commerce.pal.payment.integ.payment.financials;

import com.commerce.pal.payment.integ.payment.sahay.SahayConstants;
import com.commerce.pal.payment.module.payment.ProcessSuccessPayment;
import com.commerce.pal.payment.repo.payment.PalPaymentRepository;
import com.commerce.pal.payment.util.HttpProcessor;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

@Log
@Component
@SuppressWarnings("Duplicates")
public class FinancialPaymentFulfillment {
    @Value(value = "${org.commerce.pal.sahay.payment.fulfillment.endpoint}")
    private String URL_PAYMENT_FULFILLMENT;

    private final SahayConstants sahayConstants;
    private final HttpProcessor httpProcessor;
    private final PalPaymentRepository palPaymentRepository;
    private final ProcessSuccessPayment processSuccessPayment;

    @Autowired
    public FinancialPaymentFulfillment(SahayConstants sahayConstants,
                                       HttpProcessor httpProcessor,
                                       PalPaymentRepository palPaymentRepository,
                                       ProcessSuccessPayment processSuccessPayment) {
        this.sahayConstants = sahayConstants;
        this.httpProcessor = httpProcessor;
        this.palPaymentRepository = palPaymentRepository;
        this.processSuccessPayment = processSuccessPayment;
    }

    public JSONObject pickAndProcess(JSONObject reqBdy) {
        JSONObject respBdy = new JSONObject();
        try {
            palPaymentRepository.findPalPaymentByOrderRefAndTransRefAndStatus(
                    reqBdy.getString("OrderRef"), reqBdy.getString("TransRef"), 1
            ).ifPresentOrElse(payment -> {

                if (reqBdy.getString("PaymentStatus").equals("000")) {
                    payment.setResponsePayload(reqBdy.toString());
                    payment.setResponseDate(Timestamp.from(Instant.now()));
                    payment.setStatus(3);

                    payment.setFinalResponse("000");
                    payment.setFinalResponseMessage(reqBdy.getString("PaymentDescription"));
                    payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                    palPaymentRepository.save(payment);

                    // Process Payment
                  processSuccessPayment.pickAndProcess(payment);
                }
            }, () -> {
                respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                        .put("statusDescription", "failed")
                        .put("statusMessage", "Request failed");
            });
        } catch (Exception ex) {
            respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed")
                    .put("statusMessage", "Request failed");
        }
        return respBdy;
    }
}
