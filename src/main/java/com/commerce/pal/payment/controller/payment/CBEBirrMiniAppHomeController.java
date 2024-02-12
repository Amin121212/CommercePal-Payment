package com.commerce.pal.payment.controller.payment;

import com.commerce.pal.payment.util.HttpProcessor;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.support.SessionStatus;

import javax.servlet.http.HttpSession;

@Controller
@CrossOrigin(origins = "*")
public class CBEBirrMiniAppHomeController {
    @Value(value = "${org.cbe.birr.min.app.payment.user}")
    private String AUTH_URL;
    private final HttpProcessor httpProcessor;

    public CBEBirrMiniAppHomeController(HttpProcessor httpProcessor) {
        this.httpProcessor = httpProcessor;
    }

    @GetMapping(value = "/browse")
    public String index(@RequestHeader(value = "authorization", required = false) String authorizationHeader, HttpSession session, SessionStatus status) {
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                RequestBuilder builder = new RequestBuilder("GET");
                builder.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .setHeader(HttpHeaders.ACCEPT, "application/json")
                        .setHeader(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .setUrl(AUTH_URL)
                        .build();

                JSONObject resp = httpProcessor.jsonRequestProcessor(builder);

                if (resp.getString("StatusCode").equals("200")) {
                    session.setAttribute(session.getId(), authorizationHeader);
                    return "index.html";

                } else return "invalid-token.html";

            } else return "missing-valid-token.html";
        } catch (Exception ex) {
            status.setComplete(); // Clears the session attribute if an exception occurs
            return "server-error.html";
        }
    }
}

