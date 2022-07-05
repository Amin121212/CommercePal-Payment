package com.commerce.pal.payment.controller.payment;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/payment/v1/financial")
@Log
@RestController
@CrossOrigin(origins = "*")
@SuppressWarnings("Duplicates")
public class FinancialController {
    /*
    @Value("${org.commerce.pal.loan.request.email}")
    private String institutionEndPoint;
    @Value("${org.commerce.pal.loan.request.email}")
    private String markUpEndPoint;
    @Value("${org.commerce.pal.loan.request.email}")
    private String calculatePerProduct;
    @Value("${org.commerce.pal.loan.request.email}")
    private String calculateAll;
    @Value("${org.commerce.pal.loan.request.email}")
    private String submitOrderDetails;

     */

    // get financial institutions
    // get markups for financial institution
    // calculate the price of product
    // calculate the price for all
    // submit order details


}
