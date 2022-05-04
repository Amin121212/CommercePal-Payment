package com.commerce.pal.payment.integ.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import java.util.Properties;

@Configuration
public class EmailBeanConfig {

    @Value(value = "${spring.mail.host}")
    private String host;
    @Value(value = "${spring.mail.port}")
    private Integer port;
    @Value(value = "${spring.mail.properties.mail.transport.protocol}")
    private String transportProtocol;
    @Value(value = "${spring.mail.username}")
    private String username;
    @Value(value = "${spring.mail.password}")
    private String password;
    @Value(value = "${spring.mail.properties.mail.smtps.auth}")
    private String smtpAuth;

    @Bean
    public FreeMarkerConfigurationFactoryBean getFreeMarkerConfiguration() {
        FreeMarkerConfigurationFactoryBean bean = new FreeMarkerConfigurationFactoryBean();
        bean.setTemplateLoaderPath("classpath:templates/");
        return bean;
    }

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        //mailSender.setProtocol(transportProtocol);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
//        props.put("mail.transport.protocol", transportProtocol);
//        props.put("mail.smtp.auth", smtpAuth);
//        props.put("mail.smtp.starttls.enable", 1);
//        //props.put("mail.debug", IsDebug);
//        props.put("mail.smtp.ssl.enable", "true");

        //props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        //props.put("mail.debug", "true");
        //props.put("mail.smtp.socketFactory.fallback", "true");
        return mailSender;
    }

}