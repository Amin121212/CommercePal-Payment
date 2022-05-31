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


    /*
        PAGE CONTROLS
     */
    public static final Integer BUTTONS_TO_SHOW = 3;
    public static final Integer INITIAL_PAGE = 0;
    public static final Integer MIN_PAGE_SIZE = 5;
    public static final Integer MEDIUM_PAGE_SIZE = 7;
    public static final Integer MAX_PAGE_SIZE = 10;
    public static final Integer USSD_MAX_PAGE_SIZE=20;
    public static final Integer MAX_PAGE_SIZE_DASHBOARDS = 5;

    /*
    SHIPMENT TYPE
     */
    public static final String FromMerchantToCustomer = "M-C";
    public static final String FromMerchantToWareHouseToCustomer = "M-W-C";
    public static final String FromMerchantToWareHouseToWareHouseToCustomer = "M-W-W-C";

}
