package com.commerce.pal.payment.integ.payment.financials.rays;

import com.commerce.pal.payment.model.payment.PalPayment;
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
public class RaysLoanRequest {

    @Value(value = "${org.commerce.pal.financial.payment.rays.request}")
    private String URL_LOAN_REQUEST;
    @Value(value = "${org.commerce.pal.financial.payment.rays.confirm}")
    private String URL_LOAN_CONFIRM;

    private final HttpProcessor httpProcessor;
    private final PalPaymentRepository palPaymentRepository;

    public RaysLoanRequest(HttpProcessor httpProcessor, PalPaymentRepository palPaymentRepository) {
        this.httpProcessor = httpProcessor;
        this.palPaymentRepository = palPaymentRepository;
    }

    public JSONObject pickAndProcess(PalPayment payment) {
        JSONObject respBdy = new JSONObject();

        JSONObject payload = new JSONObject();
        JSONObject reqBdy = new JSONObject(payment.getRequestPayload());

        String phoneNumber = payment.getAccountNumber();
        String modPhoneNumber;
        if (phoneNumber != null && phoneNumber.length() >= 9)
            modPhoneNumber = "251" + phoneNumber.substring(phoneNumber.length() - 9);
        else
            throw new IllegalArgumentException("Invalid phone number format. please update your phone number");

        payload.put("FinancialCode", payment.getPaymentAccountType());
        payload.put("UserType", payment.getUserType());
        payload.put("UserId", reqBdy.getLong("UserId"));
        payload.put("UserPhoneNumber", modPhoneNumber);
        payload.put("MarkUpId", reqBdy.getLong("MarkUpId"));
        payload.put("OrderRef", payment.getOrderRef());
        payload.put("PaymentRef", payment.getTransRef());
        payload.put("Currency", payment.getCurrency());
        payload.put("LoanType", reqBdy.getString("LoanType"));
        payload.put("Amount", payment.getAmount().toString());

        palPaymentRepository.save(payment);

        log.log(Level.INFO, payload.toString());

        RequestBuilder builder = new RequestBuilder("POST");
        builder.addHeader("Content-Type", "application/json")
                .setBody(payload.toString())
                .setUrl(URL_LOAN_REQUEST)
                .build();

        JSONObject resp = httpProcessor.jsonRequestProcessor(builder);

        if (resp.getString("StatusCode").equals("200")) {
            JSONObject resBody = new JSONObject(resp.getString("ResponseBody"));
            payment.setResponsePayload(resBody.toString());
            payment.setResponseDate(Timestamp.from(Instant.now()));

            if (resBody.getString("statusCode").equals("000")) {
                respBdy = resBody;
                respBdy.put("TransRef", payment.getTransRef());

                payment.setStatus(1);
                payment.setBillTransRef(resBody.getString("LoanRef"));
                payment.setFinalResponse("0");
                payment.setFinalResponseMessage("PENDING");
                payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                palPaymentRepository.save(payment);
            } else {
                respBdy = resBody;

                payment.setStatus(5);
                payment.setBillTransRef("FAILED");
                payment.setFinalResponse("999");
                payment.setFinalResponseMessage("FAILED");
                payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                palPaymentRepository.save(payment);
            }
        } else {
            respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "Failed to process the request.")
                    .put("statusMessage", "Failed to process the request.");

            payment.setStatus(5);
            payment.setBillTransRef("FAILED");
            payment.setResponsePayload("FAILED");
            payment.setResponseDate(Timestamp.from(Instant.now()));

            payment.setFinalResponse("999");
            payment.setFinalResponseMessage("FAILED");
            payment.setFinalResponseDate(Timestamp.from(Instant.now()));
            palPaymentRepository.save(payment);
        }

        return respBdy;
    }
}
