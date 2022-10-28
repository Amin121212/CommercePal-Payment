package com.commerce.pal.payment.integ.payment.ethio;

import com.commerce.pal.payment.integ.payment.sahay.Constants;
import com.commerce.pal.payment.model.payment.MerchantWithdrawal;
import com.commerce.pal.payment.util.HttpProcessor;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class EthioSwithAccount {
    @Value(value = "${org.commerce.pal.sahay.payment.et.funds.transfer.banks.endpoint}")
    private String URL_CHECK_BANK;

    @Value(value = "${org.commerce.pal.sahay.payment.et.funds.transfer.check.endpoint}")
    private String URL_CHECK_ACCOUNT;

    private final Constants constants;
    private final HttpProcessor httpProcessor;

    public EthioSwithAccount(Constants constants, HttpProcessor httpProcessor) {
        this.constants = constants;
        this.httpProcessor = httpProcessor;
    }

    public JSONObject bankCheck() {
        JSONObject respBdy = new JSONObject();
        try {
            String accessToken = constants.getToken();
            if (accessToken == null || accessToken.equals("") || accessToken.contains("Error")) {
                respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                        .put("statusDescription", "Unable to get access token")
                        .put("statusMessage", "Unable to get access token");
            } else {

                RequestBuilder builder = new RequestBuilder("GET");
                builder.addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Content-Type", "application/json")
                        .setUrl(URL_CHECK_BANK)
                        .build();

                JSONObject resp = new JSONObject(httpProcessor.processProperRequest(builder));
                if (resp.getString("response").equals("00")) {
                    respBdy.put("statusCode", ResponseCodes.SUCCESS)
                            .put("statusDescription", "Success")
                            .put("banksBdy", resp.getJSONArray("banksBdy"))
                            .put("statusMessage", "Success");
                } else {
                    respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                            .put("statusDescription", "failed")
                            .put("statusMessage", "Request failed");
                }
            }
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
            respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed")
                    .put("statusMessage", "Request failed");
        }
        return respBdy;
    }

    public JSONObject accountCheck(JSONObject reqBdy) {
        JSONObject respBdy = new JSONObject();
        try {
            String accessToken = constants.getToken();
            if (accessToken == null || accessToken.equals("") || accessToken.contains("Error")) {
                respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                        .put("statusDescription", "Unable to get access token")
                        .put("statusMessage", "Unable to get access token");
            } else {
                JSONObject accCheck = new JSONObject();
                accCheck.put("InstId", reqBdy.getString("InstId"));
                accCheck.put("Account", reqBdy.getString("Account"));
                accCheck.put("CustomerType", 500);

                RequestBuilder builder = new RequestBuilder("POST");
                builder.addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Content-Type", "application/json")
                        .setUrl(URL_CHECK_ACCOUNT)
                        .setBody(accCheck.toString())
                        .build();

                JSONObject resp = new JSONObject(httpProcessor.processProperRequest(builder));
                if (resp.getString("response").equals("000")) {
                    respBdy.put("statusCode", ResponseCodes.SUCCESS)
                            .put("statusDescription", "Success")
                            .put("InstId", resp.getString("InstId"))
                            .put("InsName", resp.getString("InsName"))
                            .put("customerName", resp.getString("name"))
                            .put("statusMessage", "Success");
                } else {
                    respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                            .put("statusDescription", "failed")
                            .put("statusMessage", "Request failed");
                }
            }
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
            respBdy.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed")
                    .put("statusMessage", "Request failed");
        }
        return respBdy;
    }


}
