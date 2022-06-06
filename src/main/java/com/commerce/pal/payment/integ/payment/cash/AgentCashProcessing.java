package com.commerce.pal.payment.integ.payment.cash;

import com.commerce.pal.payment.model.payment.AgentCashPayment;
import com.commerce.pal.payment.model.payment.PalPayment;
import com.commerce.pal.payment.repo.payment.AgentCashPaymentRepository;
import com.commerce.pal.payment.repo.payment.PalPaymentRepository;
import com.commerce.pal.payment.util.GlobalMethods;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class AgentCashProcessing {
    private final GlobalMethods globalMethods;
    private final PalPaymentRepository palPaymentRepository;
    private final AgentCashPaymentRepository agentCashPaymentRepository;

    public AgentCashProcessing(GlobalMethods globalMethods,
                               PalPaymentRepository palPaymentRepository,
                               AgentCashPaymentRepository agentCashPaymentRepository) {
        this.globalMethods = globalMethods;
        this.palPaymentRepository = palPaymentRepository;
        this.agentCashPaymentRepository = agentCashPaymentRepository;
    }

    public JSONObject pickAndProcess(PalPayment payment) {
        JSONObject respBdy = new JSONObject();
        try {
            AtomicReference<AgentCashPayment> agentCash = new AtomicReference<>(new AgentCashPayment());
            String validationCode = globalMethods.generateValidationCode();
            agentCash.get().setOrderRef(payment.getOrderRef());
            agentCash.get().setPaymentRef(payment.getTransRef());
            agentCash.get().setCustomerId(1L);
            agentCash.get().setCustomerEmail(payment.getUserEmail());
            agentCash.get().setCurrency(payment.getCurrency());
            agentCash.get().setAmount(payment.getAmount());
            agentCash.get().setValidationCode(globalMethods.encryptCode(validationCode));
            agentCash.get().setValidationExpiryDate(Timestamp.from(Instant.now()));
            agentCash.get().setStatus(0);
            agentCash.get().setRequestedDate(Timestamp.from(Instant.now()));
            agentCashPaymentRepository.save(agentCash.get());

            payment.setResponsePayload("");
            payment.setResponseDate(Timestamp.from(Instant.now()));
            payment.setStatus(1);
            payment.setBillTransRef("");
            payment.setFinalResponse("0");
            payment.setFinalResponseMessage("PENDING");
            payment.setFinalResponseDate(Timestamp.from(Instant.now()));
            palPaymentRepository.save(payment);

            respBdy.put("statusCode", ResponseCodes.SUCCESS)
                    .put("OrderRef", payment.getOrderRef())
                    .put("TransRef", payment.getTransRef())
                    .put("ValidationCode", validationCode)
                    .put("statusDescription", "Success")
                    .put("statusMessage", "Success");

            JSONObject emailPayload = new JSONObject();
            emailPayload.put("EmailDestination", payment.getUserEmail());
            emailPayload.put("EmailSubject", "Order : [" + payment.getOrderRef() + "] - Agent Cash Payment Code");
            emailPayload.put("EmailMessage", "Validation Code : " + validationCode);
            globalMethods.processEmailWithoutTemplate(emailPayload);

        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
            respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed")
                    .put("statusMessage", "Request failed");
        }
        return respBdy;
    }
}
