package com.commerce.pal.payment.jms;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;

@Log
public class Sender {

    @Autowired
    private JmsTemplate jmsTemplate;

    public void sendAirtimePurchase(String message) {
        jmsTemplate.convertAndSend("airtime-purchase.q", message);
    }

    public void sendEbirrPayment(String message) {
        jmsTemplate.convertAndSend("e-birr-payment.q", message);
    }
}