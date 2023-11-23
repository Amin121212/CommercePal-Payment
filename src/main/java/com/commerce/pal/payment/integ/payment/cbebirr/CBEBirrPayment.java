package com.commerce.pal.payment.integ.payment.cbebirr;

import com.commerce.pal.payment.model.payment.PalPayment;
import com.commerce.pal.payment.repo.payment.PalPaymentRepository;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
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
public class CBEBirrPayment {

    @Value(value = "${org.cbe.birr.payment.url}")
    private String URL_PAYMENT_REQUEST;
    private final PalPaymentRepository palPaymentRepository;
    private final CBEBirrPaymentService cbeBirrPaymentService;

    @Autowired
    public CBEBirrPayment(PalPaymentRepository palPaymentRepository, CBEBirrPaymentService cbeBirrPaymentService) {
        this.palPaymentRepository = palPaymentRepository;
        this.cbeBirrPaymentService = cbeBirrPaymentService;
    }

    public JSONObject pickAndProcess(PalPayment payment) {
        JSONObject respBdy = new JSONObject();
        try {
            String payload = cbeBirrPaymentService.generatePaymentUrl(
                    String.valueOf(payment.getAmount()), payment.getTransRef());

            payment.setRequestPayload(payload);
            palPaymentRepository.save(payment);

            String paymentUrl = URL_PAYMENT_REQUEST + payload;

            respBdy.put("statusCode", ResponseCodes.SUCCESS)
                    .put("OrderRef", payment.getOrderRef())
                    .put("TransRef", payment.getTransRef())
                    .put("PaymentUrl", paymentUrl)
                    .put("statusDescription", "Success")
                    .put("statusMessage", "Success");


            payment.setStatus(1);
            payment.setBillTransRef(paymentUrl);
            payment.setFinalResponse("0");
            payment.setFinalResponseMessage("PENDING");
            payment.setFinalResponseDate(Timestamp.from(Instant.now()));
            palPaymentRepository.save(payment);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
            respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed")
                    .put("statusMessage", "Request failed");
        }
        return respBdy;
    }
}

