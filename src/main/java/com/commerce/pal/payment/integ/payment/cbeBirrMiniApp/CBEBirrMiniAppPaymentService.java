package com.commerce.pal.payment.integ.payment.cbeBirrMiniApp;

import com.google.common.hash.Hashing;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

@Component
@Log
public class CBEBirrMiniAppPaymentService {
    private static final String HASHING_KEY = "x9pBKzQBj45lkIuWWD0w6CZISM0lkg";
    private static final String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwaG9uZSI6IjI1MTkyOTA3MTQ4NyIsImV4cCI6MTcwNzc1MTEzMn0.fPg_YckSwq2p7FaSOd8Iaslm8KckdxwlNhl-wLcn4xk";
    @Value(value = "${org.cbe.birr.min.app.company.name}")
    private String COMPANY_NAME;
    @Value(value = "${org.cbe.birr.min.app.company.tillcode}")
    private String TILL_CODE;
    @Value(value = "${org.cbe.birr.min.app.callback.url}")
    private String CALLBACK_URL;

    public String generateHashForCbeBirr(String amount, String transactionId, String transactionTime) {
        try {
            String processedPayload =
                    "amount=" + amount +
                            "&callBackURL=" + CALLBACK_URL +
                            "&companyName=" + COMPANY_NAME +
                            "&key=" + HASHING_KEY +
                            "&tillCode=" + TILL_CODE +
                            "&token=" + token +
                            "&transactionId=" + transactionId +
                            "&transactionTime=" + transactionTime;

            // Perform SHA-256 on stringA
            return Hashing.sha256()
                    .hashString(processedPayload, StandardCharsets.UTF_8)
                    .toString();

        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage());
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public String generatePayload(String amount, String transactionId, String transactionTime, String hashedValue) {

        JSONObject payload = new JSONObject();
        payload.put("amount", amount);
        payload.put("callBackURL", CALLBACK_URL);
        payload.put("companyName", COMPANY_NAME);
        payload.put("signature", hashedValue);
        payload.put("tillCode", TILL_CODE);
        payload.put("token", token);
        payload.put("transactionId", transactionId);
        payload.put("transactionTime", transactionTime);

        return payload.toString();
    }


//    ==================MIN APP CALL BACK ======================================
//    {"paidAmount":"17251.81",
//    "paidByNumber":"0918094455",
//    "txnRef":"5",
//    "transactionId":"SBA9TE3JDQKA",
//    "tillCode":"1003800",
//    "transactionTime":"2024-02-06T16:21:24.502093200",
//    "token":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0cmFuc2FjdGlvblRpbWUiOiIyMDI0LTAyLTA2VDE2OjIxOjI0LjUwMjA5MzIwMCIsInRyYW5zYWN0aW9uSWQiOiJTQkE5VEUzSkRRS0EiLCJjYWxsQmFja1VSTCI6Imh0dHBzOi8vYXBpLmNvbW1lcmNlcGFsLmNvbToyMDk1L3BheW1lbnQvdjEvY2JlLWJpcnIvbWluLWFwcC9jYWxsLWJhY2siLCJ0aWxsQ29kZSI6IjEwMDM4MDAiLCJhbW91bnQiOiIxNzI1MS44MSIsImV4cCI6MTcxMDg2MTYxNX0.ByXMp4v_i9clFcHNcLNN2zqyqISj_dqWMhfPnG-uwLA",
//    "signature":"3aaddb536ead169cbd5c1142124aff168828773b3d726e6cdc73de7030832669"}
//
}