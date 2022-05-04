package com.commerce.pal.payment.controller;


import com.commerce.pal.payment.integ.notification.EmailClient;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Log
@CrossOrigin(origins = {"*"}, maxAge = 3600L)
@RestController
@RequestMapping({"/prime/api/v1/customer/order/payment"})
@SuppressWarnings("Duplicates")
public class CustomerOrderController {

}
