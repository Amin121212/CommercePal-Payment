package com.commerce.pal.payment.integ.payment.cash;

import com.commerce.pal.payment.model.payment.AgentCashPayment;
import com.commerce.pal.payment.model.payment.PalPayment;
import com.commerce.pal.payment.module.payment.ProcessSuccessPayment;
import com.commerce.pal.payment.module.database.PaymentStoreProcedure;
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
    private final ProcessSuccessPayment processSuccessPayment;
    private final PaymentStoreProcedure paymentStoreProcedure;
    private final AgentCashPaymentRepository agentCashPaymentRepository;

    public AgentCashProcessing(GlobalMethods globalMethods,
                               PalPaymentRepository palPaymentRepository,
                               ProcessSuccessPayment processSuccessPayment,
                               PaymentStoreProcedure paymentStoreProcedure,
                               AgentCashPaymentRepository agentCashPaymentRepository) {
        this.globalMethods = globalMethods;
        this.palPaymentRepository = palPaymentRepository;
        this.processSuccessPayment = processSuccessPayment;
        this.paymentStoreProcedure = paymentStoreProcedure;
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

            JSONObject smsBody = new JSONObject();
            smsBody.put("TemplateId", "6");
            smsBody.put("TemplateLanguage", "en");
            smsBody.put("tran_ref", agentCash.get().getPaymentRef());
            smsBody.put("order_ref", payment.getOrderRef());
            smsBody.put("OTP", validationCode);
            smsBody.put("Phone", payment.getAccountNumber().substring(payment.getAccountNumber().length() - 9));
            globalMethods.sendSMSNotification(smsBody);

            JSONObject emailPayload = new JSONObject();
            emailPayload.put("HasTemplate", "NO");
            emailPayload.put("TemplateName", "NO");
            emailPayload.put("EmailDestination", payment.getUserEmail());
            emailPayload.put("EmailSubject", "Order : [" + payment.getOrderRef() + "] - Agent Cash Payment Code");
            emailPayload.put("EmailMessage", "Trans Ref " + agentCash.get().getPaymentRef() + " and Validation Code : " + validationCode);
            globalMethods.sendEmailNotification(emailPayload);

        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
            respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed")
                    .put("statusMessage", "Request failed");
        }
        return respBdy;
    }

    public JSONObject processFulfillment(JSONObject reqBdy) {
        JSONObject respBdy = new JSONObject();
        try {
            palPaymentRepository.findPalPaymentByOrderRefAndTransRefAndStatus(
                    reqBdy.getString("OrderRef"), reqBdy.getString("TransRef"), 1
            ).ifPresentOrElse(payment -> {
                agentCashPaymentRepository.findAgentCashPaymentByOrderRefAndPaymentRefAndStatus(
                        payment.getOrderRef(), payment.getTransRef(), 0
                ).ifPresentOrElse(agentCashPayment -> {
                    reqBdy.put("Currency", agentCashPayment.getCurrency());
                    reqBdy.put("Amount", agentCashPayment.getAmount().toString());
                    reqBdy.put("PaymentNarration", "Agent Cash Payment");
                    log.log(Level.INFO, "CASH PAYMENT : " + reqBdy.toString());
                    JSONObject payRes = paymentStoreProcedure.agentCashPayment(reqBdy);
                    if (payRes.getString("Status").equals("00")) {
                        if (payRes.getString("TransactionStatus").equals("0")) {
                            agentCashPayment.setStatus(3);
                            agentCashPayment.setResponsePayload(payRes.toString());
                            agentCashPayment.setProcessingAgentId(reqBdy.getLong("AgentId"));
                            agentCashPayment.setProcessingDate(Timestamp.from(Instant.now()));
                            payment.setStatus(3);
                            payment.setFinalResponse("000");
                            payment.setFinalResponseMessage("Success Agent Payment");
                            payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                            palPaymentRepository.save(payment);

                            respBdy.put("statusCode", ResponseCodes.SUCCESS)
                                    .put("balance", payRes.getString("Balance"))
                                    .put("statusDescription", "Success")
                                    .put("statusMessage", "Success");

                            // Process Payment
                            processSuccessPayment.pickAndProcess(payment);
                        } else {
                            respBdy.put("statusCode", ResponseCodes.TRANSACTION_FAILED)
                                    .put("balance", payRes.getString("Balance"))
                                    .put("statusDescription", payRes.getString("Narration"))
                                    .put("statusMessage", "Request failed");
                        }
                    } else {
                        respBdy.put("statusCode", ResponseCodes.TRANSACTION_FAILED)
                                .put("statusDescription", payRes.getString("Message"))
                                .put("statusMessage", "Request failed");
                    }
                }, () -> {
                    respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                            .put("statusDescription", "failed")
                            .put("statusMessage", "Request failed");
                });
            }, () -> {
                respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                        .put("statusDescription", "failed")
                        .put("statusMessage", "Request failed");
            });

        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
            respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed")
                    .put("statusMessage", "Request failed");
        }
        return respBdy;
    }
}
