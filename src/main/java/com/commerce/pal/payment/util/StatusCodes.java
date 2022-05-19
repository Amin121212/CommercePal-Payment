package com.commerce.pal.payment.util;

public class StatusCodes {
    /*
    SHIPPING STATUS
     */
    public static final Integer NewOrder = 101;
    public static final Integer AcceptReadyForPickUp = 102;
    public static final Integer AssignMessengerPickAtMerchant = 103;
    public static final Integer AssignMessengerPickAtWareHouse = 104;
    public static final Integer MessengerPickedMerchantToWareHouse = 105;
    public static final Integer MessengerPickedMerchantToCustomer = 106;
    public static final Integer MessengerPickedWareHouseToCustomer = 107;
    public static final Integer MessengerDeliveredItemToCustomer = 500;
}
