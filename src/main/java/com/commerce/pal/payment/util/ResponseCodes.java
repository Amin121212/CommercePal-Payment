package com.commerce.pal.payment.util;

/**
 * @author Stephen.Okoth
 */
public class ResponseCodes {

    public static final String SUCCESS = "000";
    public static final String REGISTERED = "11";
    public static final String NOT_REGISTERED = "002";
    public static final String REGISTERED_PIN_SENT = "003";
    public static final String NOT_EXIST = "004";
    public static final String SYSTEM_LOGIN_NOT_SUCCESSFUL = "005";
    public static final String REQUEST_NOT_ACCEPTED = "006";
    public static final String ACKNOWLEDGEMENT = "007";
    public static final String USER_LOGIN_NOT_SUCCESSFUL = "009";
    public static final String PIN_MAX_TRIALS_REACHED = "012";
    public static final String INVALID_OLD_PIN = "011";
    public static final String SYSTEM_ERROR = "500";
    public static final String SYSTEM_TIMEOUT = "999";
    public static final String TRANSACTION_FAILED = "999";
    public static final String RECORD_NOT_FOUND = "801";
    public static final String CBS_TIMEOUT = "802";

    public static final String MISSING_PARAMETER = "401";
    public static final String AGENTCODE_MISSING = "402";
    public static final String REQUEST_FAILED = "440";
    public static final String DUPLICATE_transactionId = "901";


    public static final String TRANSACTION_LIMIT_NOT_SET = "600";
    public static final String DAILY_LIMIT_EXCEEDED = "601";
    public static final String TRANSACTION_LIMIT_EXCEEDED = "602";
    public static final String INVALID_AMOUNT = "603";
    public static final String BALANCE_LIMIT_EXCEEDED = "604";
    public static final String TRANSACTION_STILL_PENDING = "605";

    public static final String INSUFFICIENT_FUNDS = "008";
    public static final String INVALID_CODE = "708";
    public static final String EXPIRED_CODE = "709";

    //---------------------
    public static final String CR_ACCOUNT_MISSING = "20";
    public static final String PHONENUMBER_MISSING = "20";
    public static final String INVALID_ACCOUNT = "11";

    public static final String INSUFFICIENT_POINTS = "02";

    public static final String AGENT_DEPOSIT_REQUIRED = "52";
    public static final String DONOT_HONOR = "57";
    public static final String NO_MATCHING_RECORD = "58";
    public static final String WRONG_AMOUNT = "60";

    //public static final String LIMIT_EXCEEDED = "61";
    //public static final String DAILY_LIMIT_EXCEEDED = "61";
    public static final String WRONG_MESSAGE_FORMAT = "61";
    public static final String ACCOUNT_RELATED_ISSUES = "11";
    public static final String INVALID_MERCHANT = "10";
    public static final String MISSING_FUCNTIONALITY = "11";

    public static final String NO_CARD_RECORD = "11";
    public static final String CLOSED_ACCOUNT = "28";
    public static final String DORMANT_ACCOUNT = "52";
    public static final String NODR_ACCOUNT = "32";
    public static final String STOPPED_ACCOUNT = "17";
    public static final String NOCR_ACCOUNT = "32";

    public static final String TRANSACTION_NOT_ALLOWED = "19";
    public static final String SERVICE_UNAVAILABLE = "12";
    public static final String AGENT_CODE_ALREADY_USED = "30";
    public static final String AGENT_WRONG_IDNUMBER = "30";
    public static final String AGENT_WRONG_AGENTNUMBER = "30";
    public static final String AGENT_WRONG_REF_NUMBER = "30";

    public static final String HOST_CONNECTION_DOWN = "91";
    //public static final String ACKOWLEDGEMENT = "77";
    public static final String UNABLE_TOFETCH_LIMITS = "61";
    public static final String MISSING_TRANSACTION_IDENTIFIER = "88";
    public static final String MISSING_CUSTOMER_IDENTIFIER = "65";
    public static final String MERCHANT_TO_CUSTOMER = "M2C";
    public static final String MERCHANT_TO_BUSINESS = "M2B";
    public static final String COMMERCE_PAL_TO_BUSINESS = "CP2B";
    public static final String COMMERCE_PAL_TO_CUSTOMER = "CP2C";

}
