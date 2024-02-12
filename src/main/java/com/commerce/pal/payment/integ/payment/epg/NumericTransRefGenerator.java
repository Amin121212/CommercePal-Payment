package com.commerce.pal.payment.integ.payment.epg;

import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class NumericTransRefGenerator {

    private static final int TRANSACTION_ID_LENGTH = 18;

    public static String generate() {
        StringBuilder orderNumber = new StringBuilder();

        for (int i = 0; i < TRANSACTION_ID_LENGTH; i++) {
            int digit = ThreadLocalRandom.current().nextInt(10); // Generate a random digit (0 to 9)
            orderNumber.append(digit);
        }

        return orderNumber.toString();
    }
}

