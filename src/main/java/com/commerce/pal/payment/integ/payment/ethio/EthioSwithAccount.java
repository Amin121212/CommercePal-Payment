package com.commerce.pal.payment.integ.payment.ethio;

import com.commerce.pal.payment.integ.payment.sahay.SahayConstants;
import com.commerce.pal.payment.util.HttpProcessor;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class EthioSwithAccount {
    @Value(value = "${org.commerce.pal.sahay.payment.et.funds.transfer.banks.endpoint}")
    private String URL_CHECK_BANK;

    @Value(value = "${org.commerce.pal.sahay.payment.et.funds.transfer.check.endpoint}")
    private String URL_CHECK_ACCOUNT;

    private final SahayConstants sahayConstants;
    private final HttpProcessor httpProcessor;

    public EthioSwithAccount(SahayConstants sahayConstants, HttpProcessor httpProcessor) {
        this.sahayConstants = sahayConstants;
        this.httpProcessor = httpProcessor;
    }

    public JSONObject bankCheck() {
        JSONObject respBdy = new JSONObject();
        try {
            String accessToken = sahayConstants.getToken();
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
                    JSONArray bankList = resp.getJSONArray("banksBdy");
                    List<JSONObject> weeklyData = new ArrayList<>();
                    for (Integer ie = 0; ie < bankList.length(); ie++) {
                        JSONObject weekly = new JSONObject();
                        JSONObject json_array = bankList.optJSONObject(ie);
                        Iterator<?> keys = json_array.keys();
                        while (keys.hasNext()) {
                            String key = (String) keys.next();
                            weekly.put("InstId", key);
                            weekly.put("InstName", json_array.get(key));
                        }
                        weeklyData.add(weekly);
                    }

                    respBdy.put("statusCode", ResponseCodes.SUCCESS)
                            .put("statusDescription", "Success")
                            .put("data", weeklyData)
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
            String accessToken = sahayConstants.getToken();
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
