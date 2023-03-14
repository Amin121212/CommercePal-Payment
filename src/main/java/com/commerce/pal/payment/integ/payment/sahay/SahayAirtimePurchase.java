package com.commerce.pal.payment.integ.payment.sahay;

import com.commerce.pal.payment.repo.payment.PromotionPhoneRepository;
import com.commerce.pal.payment.util.GlobalMethods;
import com.commerce.pal.payment.util.HttpProcessor;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class SahayAirtimePurchase {
    @Value(value = "${org.commerce.pal.sahay.payment.airtime.purchase}")
    private String URL_AIRTIME_PURCHASE;

    @Value(value = "${org.commerce.pal.app.registration.airtime.amount}")
    private String PROMOTION_AMOUNT;
    private final GlobalMethods globalMethods;
    private final HttpProcessor httpProcessor;
    private final SahayConstants sahayConstants;
    private final PromotionPhoneRepository promotionPhoneRepository;

    public SahayAirtimePurchase(GlobalMethods globalMethods,
                                HttpProcessor httpProcessor,
                                SahayConstants sahayConstants,
                                PromotionPhoneRepository promotionPhoneRepository) {
        this.globalMethods = globalMethods;
        this.httpProcessor = httpProcessor;
        this.sahayConstants = sahayConstants;
        this.promotionPhoneRepository = promotionPhoneRepository;
    }

    @JmsListener(destination = "airtime-purchase.q")
    public void receive(String message) {
        log.log(Level.INFO, "Received Airtime Request : " + message);
        process(new JSONObject(message));
    }

    public void process(JSONObject reqBody) {
        try {
            log.log(Level.INFO, "Processor Received : " + reqBody.toString());
            promotionPhoneRepository.findPromotionPhoneByPhoneAndDeviceId(
                            reqBody.getString("Phone"), reqBody.getString("Device"))
                    .ifPresentOrElse(promotionPhone -> {
                        log.log(Level.INFO, promotionPhone.toString());
                        String accessToken = sahayConstants.getToken();
                        promotionPhone.setTransRef(globalMethods.generateTrans());
                        if (accessToken == null || accessToken.equals("") || accessToken.contains("Error")) {
                            log.log(Level.SEVERE, "Unable to get access token");
                            promotionPhone.setStatus(5);
                            promotionPhone.setSahayRef("FAILED");
                            promotionPhone.setResponsePayload("00");
                            promotionPhone.setProcessStatus("00");
                            promotionPhone.setProcessMessage("FAILED");
                            promotionPhone.setProcessedDate(Timestamp.from(Instant.now()));
                            promotionPhoneRepository.save(promotionPhone);
                        } else {

                            JSONObject payload = new JSONObject();
                            payload.put("PhoneNumber", promotionPhone.getPhone());
                            payload.put("BillerReference", promotionPhone.getTransRef());
                            payload.put("Amount", PROMOTION_AMOUNT);

                            RequestBuilder builder = new RequestBuilder("POST");
                            builder.addHeader("Authorization", "Bearer " + accessToken)
                                    .addHeader("Content-Type", "application/json")
                                    .setBody(payload.toString())
                                    .setUrl(URL_AIRTIME_PURCHASE)
                                    .build();
                            log.log(Level.INFO, payload.toString());
                            JSONObject resp = httpProcessor.jsonRequestProcessor(builder);
                            log.log(Level.INFO, resp.toString());
                            if (resp.getString("StatusCode").equals("200")) {
                                JSONObject resBody = new JSONObject(resp.getString("ResponseBody"));
                                promotionPhone.setResponsePayload(resBody.toString());
                                promotionPhone.setProcessedDate(Timestamp.from(Instant.now()));
                                if (resBody.getString("response").equals("000")) {
                                    promotionPhone.setSahayRef(resBody.getString("sahayRef"));
                                    promotionPhone.setStatus(3);
                                    promotionPhone.setProcessStatus("00");
                                    promotionPhone.setProcessMessage("Success");
                                    promotionPhone.setProcessedDate(Timestamp.from(Instant.now()));
                                    promotionPhoneRepository.save(promotionPhone);
                                } else {
                                    promotionPhone.setStatus(5);
                                    promotionPhone.setProcessStatus("99");
                                    promotionPhone.setProcessMessage(resBody.getString("responseDescription"));
                                    promotionPhone.setProcessedDate(Timestamp.from(Instant.now()));
                                    promotionPhoneRepository.save(promotionPhone);
                                }
                            } else {
                                promotionPhone.setStatus(5);
                                promotionPhone.setSahayRef("FAILED");
                                promotionPhone.setResponsePayload("FAILED");
                                promotionPhone.setProcessStatus("99");
                                promotionPhone.setProcessMessage("FAILED");
                                promotionPhone.setProcessedDate(Timestamp.from(Instant.now()));
                                promotionPhoneRepository.save(promotionPhone);
                            }
                        }
                    }, () -> {
                        log.log(Level.INFO, "Processor Failed : " + reqBody.toString());
                    });
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
        }
    }
}
