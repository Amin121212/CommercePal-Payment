package com.commerce.pal.payment.module.payment;

import com.commerce.pal.payment.integ.payment.financials.FinancialPayment;
import com.commerce.pal.payment.integ.payment.sahay.SahayPayment;
import com.commerce.pal.payment.model.payment.PalPayment;
import com.commerce.pal.payment.repo.payment.PalPaymentRepository;
import com.commerce.pal.payment.util.GlobalMethods;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Log
@Service
@SuppressWarnings("Duplicates")
public class PaymentService {
    private final SahayPayment sahayPayment;
    private final GlobalMethods globalMethods;
    private final FinancialPayment financialPayment;
    private final PalPaymentRepository palPaymentRepository;

    @Autowired
    public PaymentService(SahayPayment sahayPayment,
                          GlobalMethods globalMethods,
                          FinancialPayment financialPayment,
                          PalPaymentRepository palPaymentRepository) {
        this.sahayPayment = sahayPayment;
        this.globalMethods = globalMethods;
        this.financialPayment = financialPayment;
        this.palPaymentRepository = palPaymentRepository;
    }

    public JSONObject pickAndProcess(JSONObject rqBdy) {
        JSONObject respBdy = new JSONObject();
        try {
            AtomicReference<PalPayment> payment = new AtomicReference<>(new PalPayment());
            payment.get().setUserType(rqBdy.getString("UserType"));
            payment.get().setUserEmail(rqBdy.getString("UserEmail"));
            payment.get().setTransRef(globalMethods.generateTrans());
            payment.get().setOrderRef(rqBdy.getString("OrderRef"));
            payment.get().setTransType("OrderPayment");
            payment.get().setPaymentType(rqBdy.getString("PaymentType"));
            payment.get().setPaymentAccountType(rqBdy.getString("PaymentMode"));
            payment.get().setAccountNumber(rqBdy.getString("PhoneNumber"));
            payment.get().setAmount(rqBdy.getDouble("TotalAmount"));
            payment.get().setCurrency(rqBdy.getString("Currency"));
            payment.get().setStatus(0);
            payment.get().setRequestPayload(rqBdy.toString());
            payment.get().setRequestDate(Timestamp.from(Instant.now()));
            payment.set(palPaymentRepository.save(payment.get()));

            switch (rqBdy.getString("PaymentMode")) {
                case "SAHAY":
                    respBdy = sahayPayment.pickAndProcess(payment.get());
                    break;
                case "FINANCIAL":
                    respBdy = financialPayment.pickAndProcess(payment.get());
                    break;
                case "AGENT-CASH":
                    respBdy = financialPayment.pickAndProcess(payment.get());
                    break;
                default:
                    respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                            .put("statusDescription", "failed")
                            .put("statusMessage", "Request failed");
                    break;
            }
            //payment.set
        } catch (Exception ex) {
            respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed")
                    .put("statusMessage", "Request failed");
        }
        return respBdy;
    }
}
