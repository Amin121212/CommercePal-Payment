package com.commerce.pal.payment.util;

import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

@Log
@Component
public class GlobalMethods {

    public String generateTrans() {
        String ref = Timestamp.from(Instant.now()).toString();
        ref = IDGenerator.getInstance("SB").getRRN();
        return ref;
    }
}
