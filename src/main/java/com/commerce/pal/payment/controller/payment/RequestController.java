package com.commerce.pal.payment.controller.payment;

import com.commerce.pal.payment.integ.payment.amole.AmolePaymentFulfillment;
import com.commerce.pal.payment.integ.payment.cash.AgentCashProcessing;
import com.commerce.pal.payment.integ.payment.cbeBirrMiniApp.CBEBirrMiniAppPaymentFulfillment;
import com.commerce.pal.payment.integ.payment.cbebirr.CBEBirrPaymentFulfillment;
import com.commerce.pal.payment.integ.payment.epg.EPGPaymentFulfillment;
import com.commerce.pal.payment.integ.payment.ethio.EthioSwithAccount;
import com.commerce.pal.payment.integ.payment.financials.rays.RaysLoanConfirmation;
import com.commerce.pal.payment.integ.payment.hellocash.HelloCashPaymentFulfillment;
import com.commerce.pal.payment.integ.payment.sahay.SahayCustomerValidation;
import com.commerce.pal.payment.integ.payment.sahay.SahayPaymentFulfillment;
import com.commerce.pal.payment.integ.payment.telebirr.TeleBirrPaymentFulfillment;
import com.commerce.pal.payment.jms.Sender;
import com.commerce.pal.payment.module.ValidateAccessToken;
import com.commerce.pal.payment.module.payment.PaymentService;
import com.commerce.pal.payment.module.payment.PromotionService;
import com.commerce.pal.payment.util.GlobalMethods;
import com.commerce.pal.payment.util.HttpProcessor;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Level;

@RequestMapping("/payment/v1")
@Log
@RestController
@CrossOrigin(origins = "*")
@SuppressWarnings("Duplicates")
public class RequestController {
    private final Sender sender;
    private final GlobalMethods globalMethods;
    private final PaymentService paymentService;
    private final PromotionService promotionService;
    private final EthioSwithAccount ethioSwithAccount;
    private final ValidateAccessToken validateAccessToken;
    private final AgentCashProcessing agentCashProcessing;
    private final SahayCustomerValidation sahayCustomerValidation;
    private final SahayPaymentFulfillment sahayPaymentFulfillment;
    private final TeleBirrPaymentFulfillment teleBirrPaymentFulfillment;
    private final HelloCashPaymentFulfillment helloCashPaymentFulfillment;
    private final CBEBirrPaymentFulfillment cbeBirrPaymentFulfillment;
    private final EPGPaymentFulfillment epgPaymentFulfillment;
    private final AmolePaymentFulfillment amolePaymentFulfillment;
    private final CBEBirrMiniAppPaymentFulfillment cbeBirrMiniAppPaymentFulfillment;
    private final RaysLoanConfirmation raysLoanConfirmation;

    @Autowired
    public RequestController(Sender sender,
                             GlobalMethods globalMethods,
                             PaymentService paymentService,
                             PromotionService promotionService,
                             EthioSwithAccount ethioSwithAccount,
                             ValidateAccessToken validateAccessToken,
                             AgentCashProcessing agentCashProcessing,
                             SahayCustomerValidation sahayCustomerValidation,
                             SahayPaymentFulfillment sahayPaymentFulfillment,
                             TeleBirrPaymentFulfillment teleBirrPaymentFulfillment,
                             HelloCashPaymentFulfillment helloCashPaymentFulfillment,
                             CBEBirrPaymentFulfillment cbeBirrPaymentFulfillment,
                             EPGPaymentFulfillment epgPaymentFulfillment,
                             AmolePaymentFulfillment amolePaymentFulfillment,
                             CBEBirrMiniAppPaymentFulfillment cbeBirrMiniAppPaymentFulfillment,
                             HttpProcessor httpProcessor,
                             RaysLoanConfirmation raysLoanConfirmation) {
        this.sender = sender;
        this.globalMethods = globalMethods;
        this.paymentService = paymentService;
        this.promotionService = promotionService;
        this.ethioSwithAccount = ethioSwithAccount;
        this.validateAccessToken = validateAccessToken;
        this.agentCashProcessing = agentCashProcessing;
        this.sahayCustomerValidation = sahayCustomerValidation;
        this.sahayPaymentFulfillment = sahayPaymentFulfillment;
        this.teleBirrPaymentFulfillment = teleBirrPaymentFulfillment;
        this.helloCashPaymentFulfillment = helloCashPaymentFulfillment;
        this.cbeBirrPaymentFulfillment = cbeBirrPaymentFulfillment;
        this.epgPaymentFulfillment = epgPaymentFulfillment;
        this.amolePaymentFulfillment = amolePaymentFulfillment;
        this.cbeBirrMiniAppPaymentFulfillment = cbeBirrMiniAppPaymentFulfillment;
        this.raysLoanConfirmation = raysLoanConfirmation;
    }

    @PostMapping(value = "/request", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> postRequest(@RequestBody String requestBody,
                                              @RequestHeader("Authorization") String accessToken) {
        log.log(Level.INFO, requestBody);
        JSONObject responseBody = new JSONObject();
        try {
            JSONObject requestObject = new JSONObject(requestBody);
            Boolean checkToken = true;

            switch (requestObject.getString("ServiceCode")) {
                case "SAHAY-LOOKUP":
                    checkToken = false;
                    responseBody = sahayCustomerValidation.checkCustomer(requestObject.getString("PhoneNumber"));
                    break;
                case "ES-BANK-LOOKUP":
                    checkToken = false;
                    responseBody = ethioSwithAccount.bankCheck();
                    break;
                case "ES-ACCOUNT-LOOKUP":
                    checkToken = false;
                    responseBody = ethioSwithAccount.accountCheck(requestObject);
                    break;
            }
            if (checkToken.equals(true)) {
                JSONObject valTokenReq = new JSONObject();
                valTokenReq.put("AccessToken", accessToken)
                        .put("UserType", requestObject.getString("UserType"));
                JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);
                if (valTokenBdy.getString("Status").equals("00")) {
                    requestObject.put("UserEmail", valTokenBdy.getString("Email"));
                    requestObject.put("UserId", globalMethods.getUserId(requestObject.getString("UserType"), valTokenBdy.getJSONObject("UserDetails")));
                    requestObject.put("UserLanguage", valTokenBdy.getJSONObject("UserDetails").getJSONObject("Details").getString("language"));

                    switch (requestObject.getString("ServiceCode")) {
                        case "ORDER-PROMO":
                            responseBody = promotionService.pickAndProcess(requestObject);
                            break;
                        case "CHECKOUT":
                        case "LOAN-REQUEST":
                            responseBody = paymentService.pickAndProcess(requestObject);
                            break;
                        case "SAHAY-CONFIRM-PAYMENT":
                            responseBody = sahayPaymentFulfillment.pickAndProcess(requestObject);
                            break;
                        case "HELLO-CASH-CONFIRM-PAYMENT":
                            responseBody = helloCashPaymentFulfillment.pickAndProcess(requestObject);
                            break;
                        case "AGENT-CASH-FULFILLMENT":
                            responseBody = agentCashProcessing.processFulfillment(requestObject);
                            break;
                        default:
                            responseBody.put("statusCode", ResponseCodes.REQUEST_NOT_ACCEPTED)
                                    .put("statusDescription", "failed")
                                    .put("statusMessage", "Request failed");
                            break;
                    }
                }
            }
            return ResponseEntity.ok(responseBody.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
            responseBody.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed")
                    .put("statusMessage", "Request failed");
            log.log(Level.SEVERE, ex.getMessage());
            return ResponseEntity.ok(responseBody.toString());
        }
    }

    @PostMapping(value = "/rays/confirm", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> confirmRaysLoanRequest(@RequestBody String reqBody) {
        JSONObject response = raysLoanConfirmation.pickAndProcess(reqBody);
        return ResponseEntity.ok(response.toString());
    }

    @PostMapping(value = "/cbe-birr/min-app/call-back", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> postCBEBirrMinAppRes(@RequestHeader(value = "authorization") String
                                                               authorizationHeader, @RequestBody String requestBody) {
        log.log(Level.INFO, requestBody);
        JSONObject responseBody = new JSONObject();
        try {
            JSONObject requestObject = new JSONObject(requestBody);
            responseBody = cbeBirrMiniAppPaymentFulfillment.pickAndProcess(authorizationHeader, requestObject);

            //min-app expect different response codes for different cases to try again
            if (responseBody.getString("statusCode").equals("000"))
                return ResponseEntity.ok(responseBody.toString());

            return ResponseEntity.badRequest().body(responseBody.toString());
        } catch (Exception ex) {
            responseBody.put("statusCode", "501")
                    .put("statusMessage", "Request failed");
            log.log(Level.SEVERE, ex.getMessage());
            return ResponseEntity.internalServerError().body(responseBody.toString());
        }
    }

    @PostMapping(value = "/airtime-purchase", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> postSahayAirtimePurchase(@RequestBody String requestBody) {
        log.log(Level.INFO, requestBody);
        JSONObject responseBody = new JSONObject();
        try {
            sender.sendAirtimePurchase(requestBody.toString());
        } catch (Exception ex) {
            responseBody.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed")
                    .put("statusMessage", "Request failed");
            log.log(Level.SEVERE, ex.getMessage());

        }
        return ResponseEntity.ok(responseBody.toString());
    }

    @PostMapping(value = "/amole/pay", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> amolePay(@RequestBody String requestBody) {
        JSONObject responseBody = new JSONObject();
        try {
            JSONObject requestObject = new JSONObject(requestBody);
            responseBody = amolePaymentFulfillment.pickAndProcess(requestObject);
        } catch (Exception ex) {
            responseBody.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed controller")
                    .put("statusMessage", "Request failed");

            log.log(Level.SEVERE, ex.getMessage());
        }
        return ResponseEntity.ok(responseBody.toString());
    }

    @PostMapping(value = "/tele-birr/call-back", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> postTeleBirRes(@RequestBody String requestBody) {
        log.log(Level.INFO, requestBody);
        JSONObject responseBody = new JSONObject();
        try {
            responseBody = teleBirrPaymentFulfillment.pickAndProcess(requestBody);
        } catch (Exception ex) {
            responseBody.put("code", 5).put("msg", "Failed");
            log.log(Level.SEVERE, ex.getMessage());
        }
        return ResponseEntity.ok(responseBody.toString());
    }

    @PostMapping(value = "/amole/fulfillment", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> amoleFulfillment(@RequestBody String requestBody) {
        log.log(Level.INFO, requestBody);
        JSONObject responseBody = new JSONObject();
        try {
            JSONObject requestObject = new JSONObject(requestBody);
            responseBody = amolePaymentFulfillment.pickAndProcess(requestObject);
        } catch (Exception ex) {
            responseBody.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed")
                    .put("statusMessage", "Request failed");

            log.log(Level.SEVERE, ex.getMessage());
        }
        return ResponseEntity.ok(responseBody.toString());
    }

    @PostMapping(value = "/cbe-birr/call-back", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> postCBEBirrRes(@RequestBody String requestBody) {
        log.log(Level.INFO, requestBody);
        JSONObject responseBody = new JSONObject();
        try {
            JSONObject requestObject = new JSONObject(requestBody);
            responseBody = cbeBirrPaymentFulfillment.pickAndProcess(requestObject);
        } catch (Exception ex) {
            responseBody.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "Failed")
                    .put("statusMessage", "Request failed");

            log.log(Level.SEVERE, ex.getMessage());
        }
        return ResponseEntity.ok(responseBody.toString());
    }

    @PostMapping(value = "/epg/call-back", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> postEPGRes(@RequestBody String requestBody) {
        log.log(Level.INFO, requestBody);
        try {
            JSONObject requestObject = new JSONObject(requestBody);
            epgPaymentFulfillment.pickAndProcess(requestObject);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage());
        }

        return ResponseEntity.ok(new JSONObject().put("statusCode", 1)
                .put("statusDescription", "Success").toString());
    }

}


