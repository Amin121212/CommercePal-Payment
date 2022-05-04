package com.commerce.pal.payment.util;

public class TransactionStatus {
    // Order Status
    public static final Integer INITIATE = 0;
    public static final Integer PENDING_OTP_CONFIRMATION = 1;
    public static final Integer FAILED_PAYMENT_REQUEST = 2;
    public static final Integer PAYMENT_SUCCESS = 3;
    public static final Integer FAILED_IN_PAYMENT = 5;
}
