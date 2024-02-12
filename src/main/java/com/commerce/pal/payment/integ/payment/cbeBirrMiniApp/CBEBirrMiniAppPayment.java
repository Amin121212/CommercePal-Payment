package com.commerce.pal.payment.integ.payment.cbeBirrMiniApp;

import com.commerce.pal.payment.model.payment.PalPayment;
import com.commerce.pal.payment.repo.payment.PalPaymentRepository;
import com.commerce.pal.payment.util.HttpProcessor;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class CBEBirrMiniAppPayment {
    @Value(value = "${org.cbe.birr.min.app.payment.url}")
    private String URL_PAYMENT_REQUEST;
    private final HttpProcessor httpProcessor;
    private final CBEBirrMiniAppPaymentService cbeBirrMiniAppPaymentService;
    private final PalPaymentRepository palPaymentRepository;

    @Autowired
    public CBEBirrMiniAppPayment(HttpProcessor httpProcessor,
                                 CBEBirrMiniAppPaymentService cbeBirrMiniAppPaymentService, PalPaymentRepository palPaymentRepository) {
        this.httpProcessor = httpProcessor;
        this.cbeBirrMiniAppPaymentService = cbeBirrMiniAppPaymentService;
        this.palPaymentRepository = palPaymentRepository;
    }

    public JSONObject pickAndProcess(PalPayment payment) {
        JSONObject respBdy = new JSONObject();

        // Retrieve authorizationHeader
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession session = requestAttributes.getRequest().getSession();
        String authorizationHeader = null;
        if (session != null)
            authorizationHeader = (String) session.getAttribute(session.getId());

        if (authorizationHeader == null) {
            respBdy.put("statusCode", ResponseCodes.REQUEST_NOT_ACCEPTED)
                    .put("OrderRef", payment.getOrderRef())
                    .put("TransRef", payment.getTransRef())
                    .put("token", "")
                    .put("statusDescription", "This API is only accessible after being initiated via the CBE Birr Mini App.")
                    .put("statusMessage", "Failed");
        } else {
            try {
                String timestamp = String.valueOf(LocalDateTime.now());

                String hashedValue = cbeBirrMiniAppPaymentService
                        .generateHashForCbeBirr(String.valueOf(payment.getAmount()), payment.getTransRef(), timestamp);

                String reqBody = cbeBirrMiniAppPaymentService
                        .generatePayload(String.valueOf(payment.getAmount()), payment.getTransRef(), timestamp, hashedValue);

                payment.setRequestPayload(reqBody);
                palPaymentRepository.save(payment);

                RequestBuilder builder = new RequestBuilder("POST");
                builder.addHeader("Content-Type", "application/json")
                        .setHeader(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .setBody(reqBody)
                        .setUrl(URL_PAYMENT_REQUEST)
                        .build();

                JSONObject resp = httpProcessor.jsonRequestProcessor(builder);

                if (resp.getString("StatusCode").equals("200")) {

                    JSONObject resBody = new JSONObject(resp.getString("ResponseBody"));

                    payment.setResponsePayload(resBody.toString());
                    payment.setResponseDate(Timestamp.from(Instant.now()));
                    palPaymentRepository.save(payment);

                    respBdy.put("statusCode", ResponseCodes.SUCCESS)
                            .put("OrderRef", payment.getOrderRef())
                            .put("TransRef", payment.getTransRef())
                            .put("token", resBody.optString("token"))
                            .put("statusDescription", "Request processed successfully")
                            .put("statusMessage", "Success");

                    payment.setStatus(1);
                    payment.setBillTransRef(payment.getTransRef());
                    payment.setFinalResponse("0");
                    payment.setFinalResponseMessage("PENDING");
                    payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                    palPaymentRepository.save(payment);
                } else {
                    respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                            .put("token", "")
                            .put("statusDescription", "Request failed")
                            .put("statusMessage", "FAILED");

                    payment.setStatus(5);
                    payment.setBillTransRef("FAILED");
                    payment.setResponsePayload("FAILED");
                    payment.setResponseDate(Timestamp.from(Instant.now()));
                    payment.setFinalResponse("999");
                    payment.setFinalResponseMessage("FAILED");
                    payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                    palPaymentRepository.save(payment);
                    // Invalidate the session
                    session.invalidate();
                }
            } catch (Exception ex) {
                // Invalidate the session
                session.invalidate();
                log.log(Level.WARNING, ex.getMessage());
                respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                        .put("token", "")
                        .put("statusDescription", "Request failed")
                        .put("statusMessage", "FAILED");
            }
        }
        return respBdy;
    }
}
