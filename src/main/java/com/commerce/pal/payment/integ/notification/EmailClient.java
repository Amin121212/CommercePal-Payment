package com.commerce.pal.payment.integ.notification;


import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class EmailClient {


    @Qualifier("javaMailSender")
    @Autowired
    private JavaMailSender javaMailSender;

    @Qualifier("getFreeMarkerConfiguration")
    @Autowired
    private Configuration freemarkerConfig;

    @Value(value = "${org.java.email.sender}")
    private String senderEmail;

    @Async
    public String emailSender(String EmailMessage, String EmailDestination, String EmailSubject) {
        String response = "Failed";
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            String mailMessage = "";
            mailMessage = EmailMessage;

            helper.setTo(EmailDestination);
            helper.setText(mailMessage, true);
            //Default logo for JAva Self Drive
            //helper.addInline("logo.png", new ClassPathResource("/templates/logo.png"));
            helper.setSubject(EmailSubject);
            helper.setFrom(senderEmail);
            helper.setFrom(new InternetAddress(senderEmail, "Commerce Pal "));

            javaMailSender.send(message);
            response = "Success";
        } catch (Exception ex) {
            log.log(Level.INFO, ex.getMessage());
        }
        return response;
    }

    @Async
    public String emailTemplateSender(JSONObject messagePayload) {
        String response = "Failed";
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            String mailMessage = "";

            Template t = freemarkerConfig.getTemplate(messagePayload.getString("templates"));
            Map<String, Object> map =
                    new ObjectMapper().readValue(messagePayload.toString(), HashMap.class);
            mailMessage = FreeMarkerTemplateUtils.processTemplateIntoString(t, map);

            helper.setTo(messagePayload.getString("email"));
            helper.setText(mailMessage, true);
            //Default logo for JAva Self Drive
            //helper.addInline("logo.png", new ClassPathResource("/templates/logo.png"));
            helper.setSubject(messagePayload.getString("subject"));
            helper.setFrom(senderEmail);
            helper.setFrom(new InternetAddress(senderEmail, "COMMERCEPAL"));

            javaMailSender.send(message);
            response = "Success";
        } catch (Exception ex) {
            log.log(Level.INFO, ex.getMessage());
        }
        return response;
    }

}
