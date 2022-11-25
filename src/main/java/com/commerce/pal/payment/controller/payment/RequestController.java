package com.commerce.pal.payment.controller.payment;

import com.commerce.pal.payment.integ.payment.cash.AgentCashProcessing;
import com.commerce.pal.payment.integ.payment.ethio.EthioFundsTransfer;
import com.commerce.pal.payment.integ.payment.ethio.EthioSwithAccount;
import com.commerce.pal.payment.integ.payment.sahay.SahayCustomerValidation;
import com.commerce.pal.payment.integ.payment.sahay.SahayPaymentFulfillment;
import com.commerce.pal.payment.module.payment.PaymentService;
import com.commerce.pal.payment.module.ValidateAccessToken;
import com.commerce.pal.payment.module.payment.ProcessSuccessPayment;
import com.commerce.pal.payment.util.GlobalMethods;
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
    private final GlobalMethods globalMethods;
    private final PaymentService paymentService;
    private final EthioSwithAccount ethioSwithAccount;
    private final EthioFundsTransfer ethioFundsTransfer;
    private final ValidateAccessToken validateAccessToken;
    private final AgentCashProcessing agentCashProcessing;
    private final ProcessSuccessPayment processSuccessPayment;
    private final SahayCustomerValidation sahayCustomerValidation;
    private final SahayPaymentFulfillment sahayPaymentFulfillment;

    @Autowired
    public RequestController(GlobalMethods globalMethods,
                             PaymentService paymentService,
                             EthioSwithAccount ethioSwithAccount,
                             EthioFundsTransfer ethioFundsTransfer,
                             ValidateAccessToken validateAccessToken,
                             AgentCashProcessing agentCashProcessing,
                             ProcessSuccessPayment processSuccessPayment,
                             SahayCustomerValidation sahayCustomerValidation,
                             SahayPaymentFulfillment sahayPaymentFulfillment) {
        this.globalMethods = globalMethods;
        this.paymentService = paymentService;
        this.ethioSwithAccount = ethioSwithAccount;
        this.ethioFundsTransfer = ethioFundsTransfer;

        this.validateAccessToken = validateAccessToken;
        this.agentCashProcessing = agentCashProcessing;
        this.processSuccessPayment = processSuccessPayment;
        this.sahayCustomerValidation = sahayCustomerValidation;
        this.sahayPaymentFulfillment = sahayPaymentFulfillment;
    }

    @RequestMapping(value = "/request", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> postRequest(@RequestBody String requestBody,
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
                        case "CHECKOUT":
                        case "LOAN-REQUEST":
                            responseBody = paymentService.pickAndProcess(requestObject);
                            break;
                        case "SAHAY-CONFIRM-PAYMENT":
                            responseBody = sahayPaymentFulfillment.pickAndProcess(requestObject);
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
            } else {
                responseBody.put("statusCode", ResponseCodes.REQUEST_NOT_ACCEPTED)
                        .put("statusDescription", "failed")
                        .put("statusMessage", "Request failed");
            }
            return ResponseEntity.ok(responseBody.toString());

        } catch (Exception ex) {
            responseBody.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed")
                    .put("statusMessage", "Request failed");
            log.log(Level.SEVERE, ex.getMessage());
            return ResponseEntity.ok(responseBody.toString());
        }
    }
}
