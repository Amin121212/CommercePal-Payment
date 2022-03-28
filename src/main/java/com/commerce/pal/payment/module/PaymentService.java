package com.commerce.pal.payment.module;

import com.commerce.pal.payment.model.PalPayment;
import com.commerce.pal.payment.repo.PalPaymentRepository;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log
@Service
@SuppressWarnings("Duplicates")
public class PaymentService {
    private final PalPaymentRepository palPaymentRepository;

    @Autowired
    public PaymentService(PalPaymentRepository palPaymentRepository) {
        this.palPaymentRepository = palPaymentRepository;
    }

    public JSONObject pickAndProcess(JSONObject rqBdy) {
        JSONObject respBdy = new JSONObject();
        try {
            PalPayment payment = new PalPayment();
            payment.setUserType("C");
            //payment.set
        } catch (Exception ex) {

        }
        return respBdy;
    }
}
