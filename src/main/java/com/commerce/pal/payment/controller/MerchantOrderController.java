package com.commerce.pal.payment.controller;

import com.commerce.pal.payment.module.DataAccessService;
import com.commerce.pal.payment.module.ValidateAccessToken;
import com.commerce.pal.payment.repo.payment.OrderItemRepository;
import com.commerce.pal.payment.repo.payment.OrderRepository;
import com.commerce.pal.payment.util.GlobalMethods;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Log
@CrossOrigin(origins = {"*"}, maxAge = 3600L)
@RestController
@RequestMapping({"/prime/api/v1/merchant/order"})
@SuppressWarnings("Duplicates")
public class MerchantOrderController {
    private final GlobalMethods globalMethods;
    private final OrderRepository orderRepository;
    private final DataAccessService dataAccessService;
    private final OrderItemRepository orderItemRepository;
    private final ValidateAccessToken validateAccessToken;

    @Autowired
    public MerchantOrderController(GlobalMethods globalMethods,
                                   OrderRepository orderRepository,
                                   DataAccessService dataAccessService,
                                   OrderItemRepository orderItemRepository,
                                   ValidateAccessToken validateAccessToken) {
        this.globalMethods = globalMethods;
        this.orderRepository = orderRepository;
        this.dataAccessService = dataAccessService;
        this.orderItemRepository = orderItemRepository;
        this.validateAccessToken = validateAccessToken;
    }
    /*
    0-New,1-MerchantRelese,2-WareHouseRelease,3-MessengerRelease,4-Delivered
     */

    @RequestMapping(value = {"/order-summary"}, method = {RequestMethod.GET}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> orderSummary(@RequestHeader("Authorization") String accessToken,
                                          @RequestParam("status") Integer status) {
        JSONObject responseMap = new JSONObject();
        JSONObject valTokenReq = new JSONObject();
        valTokenReq.put("AccessToken", accessToken)
                .put("UserType", "M");

        JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);

        if (valTokenBdy.getString("Status").equals("00")) {
            JSONObject userDetails = valTokenBdy.getJSONObject("UserDetails");
            JSONObject merchantInfo = userDetails.getJSONObject("merchantInfo");// userDetails.getJSONObject("messengerInfo");
            Long merchantId = Long.valueOf(userDetails.getJSONObject("merchantInfo").getInt("userId"));
            List<Integer> shipmentStatus = new ArrayList<>();
            if (status.equals(10)) {
                shipmentStatus.add(0);
                shipmentStatus.add(1);
                shipmentStatus.add(2);
                shipmentStatus.add(3);
                shipmentStatus.add(4);
                shipmentStatus.add(5);
            } else {
                shipmentStatus.add(status);
            }
            List<JSONObject> orders = new ArrayList<>();
            orderItemRepository.findByMerchantIdAndShipmentStatus(merchantId, shipmentStatus)
                    .forEach(orderItemParent -> {
                        orderRepository.findOrderByOrderId(orderItemParent)
                                .ifPresent(order -> {
                                    JSONObject orderDetails = new JSONObject();
                                    JSONObject cusReq = new JSONObject();
                                    cusReq.put("Type", "CUSTOMER");
                                    cusReq.put("TypeId", order.getCustomerId());
                                    JSONObject cusRes = dataAccessService.pickAndProcess(cusReq);
                                    orderDetails.put("CustomerName", cusRes.getString("firstName"));
                                    orderDetails.put("OrderRef", order.getOrderRef());
                                    orderDetails.put("OrderDate", order.getOrderDate());

                                    List<JSONObject> orderItems = new ArrayList<>();
                                    orderItemRepository.findOrderItemsByOrderIdAndMerchantId(order.getOrderId(), merchantId)
                                            .forEach(orderItem -> {
                                                JSONObject itemPay = new JSONObject();
                                                JSONObject prodReq = new JSONObject();
                                                prodReq.put("Type", "PRODUCT");
                                                prodReq.put("TypeId", orderItem.getProductLinkingId());
                                                itemPay = dataAccessService.pickAndProcess(prodReq);
                                                itemPay.put("NoOfProduct", orderItem.getQuantity());
                                                itemPay.put("ItemOrderRef", orderItem.getSubOrderNumber());
                                                orderItems.add(itemPay);
                                            });
                                    orderDetails.put("orderItems", orderItems);
                                    orders.add(orderDetails);
                                });
                    });
            responseMap.put("statusCode", ResponseCodes.SUCCESS)
                    .put("statusDescription", "success")
                    .put("data", orders)
                    .put("statusMessage", "Request Successful");
        } else {
            responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                    .put("statusDescription", "Merchant Does not exists")
                    .put("statusMessage", "Merchant Does not exists");
        }
        return ResponseEntity.ok(responseMap.toString());
    }
}
