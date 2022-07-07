package com.commerce.pal.payment.controller.payment;

import com.commerce.pal.payment.module.DataAccessService;
import com.commerce.pal.payment.repo.payment.OrderItemRepository;
import com.commerce.pal.payment.repo.payment.OrderRepository;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@RequestMapping("/payment/v1/financial")
@Log
@RestController
@CrossOrigin(origins = "*")
@SuppressWarnings("Duplicates")
public class FinancialController {
    private final OrderRepository orderRepository;
    private final DataAccessService dataAccessService;
    private final OrderItemRepository orderItemRepository;

    @Autowired
    public FinancialController(OrderRepository orderRepository,
                               DataAccessService dataAccessService,
                               OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.dataAccessService = dataAccessService;
        this.orderItemRepository = orderItemRepository;
    }

    @RequestMapping(value = {"/order-detail"}, method = {RequestMethod.GET}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> orderDetails(@RequestParam("orderRef") String orderRef) {
        JSONObject responseMap = new JSONObject();

        JSONObject orderDetails = new JSONObject();
        orderRepository.findOrderByOrderRef(orderRef)
                .ifPresent(order -> {
                    orderDetails.put("OrderRef", order.getOrderRef());
                    orderDetails.put("OrderDate", order.getOrderDate());
                    orderDetails.put("DeliveryPrice", order.getDeliveryPrice());
                    orderDetails.put("TotalPrice", order.getTotalPrice());
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
                });
        responseMap.put("statusCode", ResponseCodes.SUCCESS)
                .put("statusDescription", "success")
                .put("data", orderDetails)
                .put("statusMessage", "Request Successful");

        return ResponseEntity.ok(responseMap.toString());
    }

    @RequestMapping(value = "/financial-callback", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> financialCallback(@RequestBody String requestBody) {
        log.log(Level.INFO, requestBody);
        JSONObject responseBody = new JSONObject();
        try {
            JSONObject requestObject = new JSONObject(requestBody);
//            processSuccessPayment.pickAndProcess(requestObject.getString("OrderRef"));
            return ResponseEntity.ok(responseBody.toString());
        } catch (Exception ex) {
            responseBody.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed")
                    .put("statusMessage", "Request failed");
            log.log(Level.SEVERE, ex.getMessage());
            return ResponseEntity.ok(responseBody.toString());
        }
    }


}
