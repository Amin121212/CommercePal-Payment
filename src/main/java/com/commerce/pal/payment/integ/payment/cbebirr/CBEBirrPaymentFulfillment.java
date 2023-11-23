package com.commerce.pal.payment.integ.payment.cbebirr;

import com.commerce.pal.payment.module.payment.ProcessSuccessPayment;
import com.commerce.pal.payment.repo.payment.PalPaymentRepository;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Log
@Component
@SuppressWarnings("Duplicates")
public class CBEBirrPaymentFulfillment {

    private final PalPaymentRepository palPaymentRepository;
    private final ProcessSuccessPayment processSuccessPayment;

    @Autowired
    public CBEBirrPaymentFulfillment(PalPaymentRepository palPaymentRepository,
                                     ProcessSuccessPayment processSuccessPayment, CBEBirrUtils cbeBirrUtils) {
        this.palPaymentRepository = palPaymentRepository;
        this.processSuccessPayment = processSuccessPayment;
    }

    //TODO: Update it after knowing what the values of the success message from CBE Birr are.
    public JSONObject pickAndProcess(JSONObject reqBdy) {
//        JSONObject respBdy = new JSONObject();
//        try {
//            String transactionId = CBEBirrUtils.decrypt(reqBdy.getString("TransactionId"));
//            String state = CBEBirrUtils.decrypt(reqBdy.getString("State"));
//
//
//            palPaymentRepository.findPalPaymentByTransRefAndStatus(transactionId, 1
//            ).ifPresentOrElse(payment -> {
//                        payment.setResponsePayload(reqBdy.toString());
//                        payment.setResponseDate(Timestamp.from(Instant.now()));
//                        if (state.equals("success")) {
//                            payment.setStatus(3);
//                            payment.setFinalResponse("000");
//                            payment.setFinalResponseMessage("SUCCESS");
//                            payment.setFinalResponseDate(Timestamp.from(Instant.now()));
//                            palPaymentRepository.save(payment);
//                            respBdy.put("statusCode", ResponseCodes.SUCCESS)
//                                    .put("billRef", payment.getOrderRef())
//                                    .put("TransRef", payment.getTransRef())
//                                    .put("statusDescription", "Success")
//                                    .put("statusMessage", "Success");
//
//                            // Process Payment
//                            processSuccessPayment.pickAndProcess(payment);
//                        } else {
//                            payment.setStatus(5);
//                            payment.setBillTransRef("FAILED");
//                            payment.setFinalResponse("999");
//                            payment.setFinalResponseMessage("FAILED");
//                            payment.setFinalResponseDate(Timestamp.from(Instant.now()));
//                            palPaymentRepository.save(payment);
//
//                            respBdy.put("statusCode", ResponseCodes.TRANSACTION_FAILED)
//                                    .put("statusDescription", "failed")
//                                    .put("statusMessage", "Request failed");
//                        }
//                    }
//                    , () -> {
//                        respBdy.put("statusCode", ResponseCodes.TRANSACTION_FAILED)
//                                .put("statusDescription", "failed")
//                                .put("statusMessage", "Request failed");
//                    });
//        } catch (Exception ex) {
//            log.log(Level.WARNING, ex.getMessage());
//            respBdy.put("statusCode", ResponseCodes.TRANSACTION_FAILED)
//                    .put("statusDescription", "failed")
//                    .put("statusMessage", ex.getMessage());
//        }

        System.out.println("CBE CALL BACK RESULT:  " + reqBdy.toString());
        return new JSONObject().put("statusCode", ResponseCodes.SUCCESS);
    }
}
