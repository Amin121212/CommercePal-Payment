package com.commerce.pal.payment.controller.payment;

import com.commerce.pal.payment.integ.payment.sahay.SahayCustomerValidation;
import com.commerce.pal.payment.integ.payment.sahay.SahayPaymentFulfillment;
import com.commerce.pal.payment.module.payment.PaymentService;
import com.commerce.pal.payment.module.ValidateAccessToken;
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
    private final PaymentService paymentService;
    private final ValidateAccessToken validateAccessToken;
    private final SahayCustomerValidation sahayCustomerValidation;
    private final SahayPaymentFulfillment sahayPaymentFulfillment;

    @Autowired
    public RequestController(PaymentService paymentService,
                             ValidateAccessToken validateAccessToken,
                             SahayCustomerValidation sahayCustomerValidation,
                             SahayPaymentFulfillment sahayPaymentFulfillment) {
        this.paymentService = paymentService;

        this.validateAccessToken = validateAccessToken;
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
            JSONObject valTokenReq = new JSONObject();
            valTokenReq.put("AccessToken", accessToken)
                    .put("UserType", requestObject.getString("UserType"));

            JSONObject valTokenBdy = validateAccessToken.pickAndProcess(valTokenReq);

            if (valTokenBdy.getString("Status").equals("00")) {
                requestObject.put("UserEmail", valTokenBdy.getString("Email"));
                switch (requestObject.getString("ServiceCode")) {
                    case "SAHAY-LOOKUP":
                        responseBody = sahayCustomerValidation.checkCustomer(requestObject.getString("PhoneNumber"));
                        break;
                    case "CHECKOUT":
                        responseBody = paymentService.pickAndProcess(requestObject);
                        break;
                    case "SAHAY-CONFIRM-PAYMENT":
                        responseBody = sahayPaymentFulfillment.pickAndProcess(requestObject);
                        break;
                    default:
                        responseBody.put("statusCode", ResponseCodes.REQUEST_NOT_ACCEPTED)
                                .put("statusDescription", "failed")
                                .put("statusMessage", "Request failed");
                        break;
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
