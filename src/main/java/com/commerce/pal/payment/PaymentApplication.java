package com.commerce.pal.payment;

import lombok.extern.java.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.Transactional;

@Log
@EnableAsync
@Transactional
@EnableScheduling
@SpringBootApplication
@SuppressWarnings("Duplicates")
@PropertySources({
		@PropertySource(value = "file:E:\\Apps\\CommercePalPayment\\application.properties", ignoreResourceNotFound = true)
})
public class PaymentApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentApplication.class, args);
	}

}
