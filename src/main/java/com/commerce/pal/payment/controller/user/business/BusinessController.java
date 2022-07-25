package com.commerce.pal.payment.controller.user.business;

import com.commerce.pal.payment.module.DataAccessService;
import com.commerce.pal.payment.module.ValidateAccessToken;
import com.commerce.pal.payment.module.order.OrderService;
import com.commerce.pal.payment.repo.LoginValidationRepository;
import com.commerce.pal.payment.repo.payment.OrderItemRepository;
import com.commerce.pal.payment.repo.payment.OrderRepository;
import com.commerce.pal.payment.repo.shipping.ItemMessengerDeliveryRepository;
import com.commerce.pal.payment.util.GlobalMethods;
import com.commerce.pal.payment.util.HttpProcessor;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Log
@CrossOrigin(origins = {"*"}, maxAge = 3600L)
@RestController
@RequestMapping({"/prime/api/v1/business/order"})
@SuppressWarnings("Duplicates")
public class BusinessController {

    @Value(value = "${org.commerce.pal.financial.business.loan}")
    private String LOAN_URL;

    private final OrderService orderService;
    private final GlobalMethods globalMethods;
    private final HttpProcessor httpProcessor;
    private final OrderRepository orderRepository;
    private final DataAccessService dataAccessService;
    private final OrderItemRepository orderItemRepository;
    private final ValidateAccessToken validateAccessToken;
    private final LoginValidationRepository loginValidationRepository;
    private final ItemMessengerDeliveryRepository itemMessengerDeliveryRepository;

    @Autowired
    public BusinessController(OrderService orderService,
                              GlobalMethods globalMethods,
                              HttpProcessor httpProcessor,
                              OrderRepository orderRepository,
                              DataAccessService dataAccessService,
                              OrderItemRepository orderItemRepository,
                              ValidateAccessToken validateAccessToken,
                              LoginValidationRepository loginValidationRepository,
                              ItemMessengerDeliveryRepository itemMessengerDeliveryRepository) {
        this.orderService = orderService;
        this.globalMethods = globalMethods;
        this.httpProcessor = httpProcessor;
        this.orderRepository = orderRepository;
        this.dataAccessService = dataAccessService;
        this.orderItemRepository = orderItemRepository;
        this.validateAccessToken = validateAccessToken;
        this.loginValidationRepository = loginValidationRepository;
        this.itemMessengerDeliveryRepository = itemMessengerDeliveryRepository;
    }

    @RequestMapping(value = {"/order-detail"}, method = {RequestMethod.GET}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> orderDetails(@RequestHeader("Authorization") String accessToken) {
        JSONObject responseMap = new JSONObject();

        List<JSONObject> orders = new ArrayList<>();
        JSONObject valTokenReq = new JSONObject();
        valTokenReq.put("AccessToken", accessToken)
                .put("UserType", "B");

        JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);

        if (valTokenBdy.getString("Status").equals("00")) {
            JSONObject userDetails = valTokenBdy.getJSONObject("UserDetails");
            JSONObject businessInfo = userDetails.getJSONObject("businessInfo");
            Long businessId = Long.valueOf(userDetails.getJSONObject("businessInfo").getInt("userId"));

            orderRepository.findOrdersByBusinessId(businessId)
                    .forEach(order -> {
                        JSONObject orderDetails = new JSONObject();
                        orderDetails.put("OrderRef", order.getOrderRef());
                        orderDetails.put("OrderDate", order.getOrderDate());
                        orderDetails.put("DeliveryPrice", order.getDeliveryPrice());
                        orderDetails.put("TotalPrice", order.getTotalPrice());
                        orderDetails.put("PaymentStatus", order.getPaymentStatus());
                        orderDetails.put("PaymentDate", order.getPaymentDate());
                        orderDetails.put("PaymentMethod", order.getPaymentMethod());
                        orderDetails.put("Discount", order.getDiscount());
                        orderDetails.put("DeliveryPrice", order.getDeliveryPrice());
                        List<JSONObject> orderItems = new ArrayList<>();
                        orderItemRepository.findOrderItemsByOrderId(order.getOrderId())
                                .forEach(orderItem -> {
                                    JSONObject prodReq = new JSONObject();
                                    prodReq.put("Type", "PRODUCT-AND-SUB");
                                    prodReq.put("TypeId", orderItem.getProductLinkingId());
                                    prodReq.put("SubProductId", orderItem.getSubProductId());
                                    JSONObject prodRes = dataAccessService.pickAndProcess(prodReq);
                                    JSONObject itemPay = orderService.orderItemDetails(orderItem.getItemId());
                                    itemPay.put("Product", prodRes);
                                    orderItems.add(itemPay);
                                });
                        orderDetails.put("orderItems", orderItems);
                        orders.add(orderDetails);
                    });
        }
        responseMap.put("statusCode", ResponseCodes.SUCCESS)
                .put("statusDescription", "success")
                .put("data", orders)
                .put("statusMessage", "Request Successful");

        return ResponseEntity.ok(responseMap.toString());
    }

    @RequestMapping(value = "/get-loan-request", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> businessGetLoan(@RequestHeader("Authorization") String accessToken) {
        JSONObject responseBody = new JSONObject();
        try {
            JSONObject valTokenReq = new JSONObject();
            valTokenReq.put("AccessToken", accessToken)
                    .put("UserType", "B");

            JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);

            if (valTokenBdy.getString("Status").equals("00")) {
                JSONObject userDetails = valTokenBdy.getJSONObject("UserDetails");
                Long businessId = Long.valueOf(userDetails.getJSONObject("businessInfo").getInt("userId"));

                JSONObject payload = new JSONObject();
                payload.put("BusinessId", businessId.toString());
                RequestBuilder builder = new RequestBuilder("POST");
                builder.addHeader("Content-Type", "application/json")
                        .setBody(payload.toString())
                        .setUrl(LOAN_URL)
                        .build();
                JSONObject resp = httpProcessor.jsonRequestProcessor(builder);
                if (resp.getString("StatusCode").equals("200")) {
                    responseBody = new JSONObject(resp.getString("ResponseBody"));
                } else {
                    responseBody.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                            .put("statusDescription", "failed")
                            .put("statusMessage", "Request failed");
                }
            } else {
                responseBody.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                        .put("statusDescription", "failed")
                        .put("statusMessage", "Request failed");
            }
        } catch (Exception ex) {
            responseBody.put("statusCode", "401")
                    .put("statusDescription", ex.getMessage())
                    .put("statusMessage", ex.getMessage());
            log.log(Level.SEVERE, ex.getMessage());
        }
        return ResponseEntity.ok(responseBody.toString());
    }


    @RequestMapping(value = "/generate-otp-code", method = RequestMethod.POST)
    public ResponseEntity<?> generateOtpCode(@RequestHeader("Authorization") String accessToken,
                                             @RequestBody String req) {
        JSONObject responseMap = new JSONObject();
        try {
            JSONObject valTokenReq = new JSONObject();
            valTokenReq.put("AccessToken", accessToken)
                    .put("UserType", "B");
            JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);
            if (valTokenBdy.getString("Status").equals("00")) {
                JSONObject userDetails = valTokenBdy.getJSONObject("UserDetails");
                JSONObject businessInfo = userDetails.getJSONObject("businessInfo");
                Long businessId = Long.valueOf(businessInfo.getInt("userId"));
                JSONObject request = new JSONObject(req);

                itemMessengerDeliveryRepository.findItemMessengerDeliveryByOrderItemIdAndCustomerId(request.getLong("OrderItemId"), businessId
                ).ifPresentOrElse(itemMessengerDelivery -> {
                    orderItemRepository.findById(request.getLong("OrderItemId"))
                            .ifPresentOrElse(orderItem -> {
                                String validationCode = globalMethods.generateValidationCode();
                                itemMessengerDelivery.setValidationCode(globalMethods.encryptCode(validationCode));
                                itemMessengerDelivery.setValidationStatus(0);

                                orderItemRepository.save(orderItem);

                                loginValidationRepository.findLoginValidationByEmailAddress(businessInfo.getString("email"))
                                        .ifPresent(user -> {
                                            JSONObject pushPayload = new JSONObject();
                                            pushPayload.put("UserId", user.getUserOneSignalId() != null ? user.getUserOneSignalId() : "5c66ca50-c009-480f-a200-72c244d74ff4");
                                            pushPayload.put("Header", "Generate Code for : " + orderItem.getSubOrderNumber());
                                            pushPayload.put("Message", "Generate Code for : " + orderItem.getSubOrderNumber());
                                            JSONObject data = new JSONObject();
                                            data.put("OrderItem", orderItem.getSubOrderNumber());
                                            pushPayload.put("data", data);
                                            globalMethods.sendPushNotification(pushPayload);
                                        });

                                responseMap.put("statusCode", ResponseCodes.SUCCESS)
                                        .put("statusDescription", "Success")
                                        .put("ValidCode", validationCode)
                                        .put("statusMessage", "Success");
                            }, () -> {
                                responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                                        .put("statusDescription", "The Item does not exists")
                                        .put("statusMessage", "The Item does not exists");
                            });
                }, () -> {
                    responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                            .put("statusDescription", "The Delivery is not assigned to this messenger")
                            .put("statusMessage", "The Delivery is not assigned to this messenger");
                });
            } else {
                responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                        .put("statusDescription", "Error in user validation")
                        .put("statusMessage", "Error in user validation");
            }
        } catch (Exception ex) {
            responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                    .put("statusDescription", "Error in user validation")
                    .put("statusMessage", "Error in user validation");
            log.log(Level.WARNING, ex.getMessage());
        }
        return ResponseEntity.ok(responseMap.toString());
    }

}
