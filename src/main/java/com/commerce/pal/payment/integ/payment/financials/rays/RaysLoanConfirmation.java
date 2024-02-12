package com.commerce.pal.payment.integ.payment.financials.rays;

import com.commerce.pal.payment.module.payment.ProcessSuccessPayment;
import com.commerce.pal.payment.repo.payment.PalPaymentRepository;
import com.commerce.pal.payment.util.HttpProcessor;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Level;

@Log
@Component
public class RaysLoanConfirmation {
    @Value(value = "${org.commerce.pal.financial.payment.rays.confirm}")
    private String URL_LOAN_CONFIRM;
    private final PalPaymentRepository palPaymentRepository;
    private final HttpProcessor httpProcessor;
    private final ProcessSuccessPayment processSuccessPayment;

    public RaysLoanConfirmation(PalPaymentRepository palPaymentRepository, HttpProcessor httpProcessor, ProcessSuccessPayment processSuccessPayment) {
        this.palPaymentRepository = palPaymentRepository;
        this.httpProcessor = httpProcessor;
        this.processSuccessPayment = processSuccessPayment;
    }

    public JSONObject pickAndProcess(String request) {
        final JSONObject[] response = {new JSONObject()};  // Using an array to make it effectively final
        JSONObject reqBdy = new JSONObject(request);
        palPaymentRepository.findPalPaymentByTransRefAndStatus(reqBdy.getString("TransRef"), 1)
                .ifPresentOrElse(payment -> {
                    JSONObject payload = new JSONObject();
                    payload.put("LoanRef", reqBdy.getString("LoanRef"));
                    payload.put("otp", reqBdy.get("otp"));

                    payment.setRequestPayload(payload.toString());
                    palPaymentRepository.save(payment);

                    log.log(Level.INFO, payload.toString());

                    RequestBuilder builder = new RequestBuilder("POST");
                    builder.addHeader("Content-Type", "application/json")
                            .setBody(payload.toString())
                            .setUrl(URL_LOAN_CONFIRM)
                            .build();

                    JSONObject resp = httpProcessor.jsonRequestProcessor(builder);

                    if (resp.getString("StatusCode").equals("200")) {
                        JSONObject resBody = new JSONObject(resp.getString("ResponseBody"));
                        payment.setResponsePayload(resBody.toString());
                        payment.setResponseDate(Timestamp.from(Instant.now()));
                        palPaymentRepository.save(payment);

                        if (resBody.getString("statusCode").equals("000")) {
                            response[0] = resBody;

                            payment.setStatus(3);
                            payment.setFinalResponse("000");
                            payment.setFinalResponseMessage(resBody.getString("statusMessage"));
                            payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                            palPaymentRepository.save(payment);

                            // Process Payment
                            processSuccessPayment.pickAndProcess(payment);
                        } else {
                            response[0] = resBody;

                            // don't update payment status. give user another chance
                            payment.setFinalResponseMessage(resBody.getString("statusMessage"));
                            payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                            palPaymentRepository.save(payment);
                        }
                    } else {
                        response[0].put("statusCode", ResponseCodes.SYSTEM_ERROR)
                                .put("statusDescription", "Failed to process the request.")
                                .put("statusMessage", "Failed to process the request.");

                        payment.setStatus(5);
                        payment.setResponsePayload("FAILED");
                        payment.setResponseDate(Timestamp.from(Instant.now()));
                        payment.setFinalResponse("999");
                        payment.setFinalResponseMessage("FAILED");
                        payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                        palPaymentRepository.save(payment);
                    }
                }, () -> response[0].put("statusCode", ResponseCodes.TRANSACTION_FAILED)
                        .put("statusDescription", "Unable to process your Confirmation request.")
                        .put("statusMessage", "Pending Payment not found with the given reference: " + reqBdy.getString("TransRef")));

        return response[0];
    }
}



