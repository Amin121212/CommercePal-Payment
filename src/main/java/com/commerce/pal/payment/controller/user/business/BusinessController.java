package com.commerce.pal.payment.controller.user.business;

import com.commerce.pal.payment.module.DataAccessService;
import com.commerce.pal.payment.module.ValidateAccessToken;
import com.commerce.pal.payment.repo.payment.OrderItemRepository;
import com.commerce.pal.payment.repo.payment.OrderRepository;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Log
@CrossOrigin(origins = {"*"}, maxAge = 3600L)
@RestController
@RequestMapping({"/prime/api/v1/business/order"})
@SuppressWarnings("Duplicates")
public class BusinessController {
    private final OrderRepository orderRepository;
    private final DataAccessService dataAccessService;
    private final OrderItemRepository orderItemRepository;
    private final ValidateAccessToken validateAccessToken;

    public BusinessController(OrderRepository orderRepository,
                              DataAccessService dataAccessService,
                              OrderItemRepository orderItemRepository,
                              ValidateAccessToken validateAccessToken) {
        this.orderRepository = orderRepository;
        this.dataAccessService = dataAccessService;
        this.orderItemRepository = orderItemRepository;
        this.validateAccessToken = validateAccessToken;
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
                                    JSONObject itemPay = new JSONObject();
                                    JSONObject prodReq = new JSONObject();
                                    prodReq.put("Type", "PRODUCT-AND-SUB");
                                    prodReq.put("TypeId", orderItem.getProductLinkingId());
                                    prodReq.put("SubProductId", orderItem.getSubProductId());
                                    JSONObject prodRes = dataAccessService.pickAndProcess(prodReq);
                                    itemPay.put("NoOfProduct", orderItem.getQuantity());
                                    itemPay.put("ItemOrderRef", orderItem.getSubOrderNumber());
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
}
