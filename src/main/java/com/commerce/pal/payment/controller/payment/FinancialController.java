package com.commerce.pal.payment.controller.payment;

import com.commerce.pal.payment.module.DataAccessService;
import com.commerce.pal.payment.module.payment.ProcessSuccessPayment;
import com.commerce.pal.payment.repo.payment.OrderItemRepository;
import com.commerce.pal.payment.repo.payment.OrderRepository;
import com.commerce.pal.payment.repo.payment.PalPaymentRepository;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
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
    private final PalPaymentRepository palPaymentRepository;
    private final ProcessSuccessPayment processSuccessPayment;

    @Autowired
    public FinancialController(OrderRepository orderRepository,
                               DataAccessService dataAccessService,
                               OrderItemRepository orderItemRepository,
                               PalPaymentRepository palPaymentRepository,
                               ProcessSuccessPayment processSuccessPayment) {
        this.orderRepository = orderRepository;
        this.dataAccessService = dataAccessService;
        this.orderItemRepository = orderItemRepository;
        this.palPaymentRepository = palPaymentRepository;
        this.processSuccessPayment = processSuccessPayment;
    }

    @RequestMapping(value = {"/order-detail"}, method = {RequestMethod.POST}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> orderDetails(@RequestBody String requestBody) {
        JSONObject responseMap = new JSONObject();

        JSONObject orderDetails = new JSONObject();
        JSONObject requestObject = new JSONObject(requestBody);
        orderRepository.findOrderByOrderRef(requestObject.getString("OrderRef"))
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
            JSONObject reqBody = new JSONObject(requestBody);
            palPaymentRepository.findPalPaymentByOrderRefAndTransRefAndStatus(
                    reqBody.getString("OrderRef"), reqBody.getString("TransRef"), 1
            ).ifPresentOrElse(payment -> {
                payment.setResponsePayload(reqBody.toString());
                payment.setResponseDate(Timestamp.from(Instant.now()));
                if (reqBody.getString("PaymentStatus").equals("000")) {
                    responseBody.put("statusCode", ResponseCodes.SUCCESS)
                            .put("OrderRef", payment.getOrderRef())
                            .put("TransRef", payment.getTransRef())
                            .put("statusDescription", "Success")
                            .put("statusMessage", "Success");

                    payment.setStatus(3);
                    payment.setFinalResponse("000");
                    payment.setFinalResponseMessage(reqBody.getString("PaymentDescription"));
                    payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                    palPaymentRepository.save(payment);

                    // Process Payment
                    processSuccessPayment.pickAndProcess(payment);
                } else {
                    responseBody.put("statusCode", ResponseCodes.TRANSACTION_FAILED)
                            .put("statusDescription", "failed")
                            .put("statusMessage", "Request failed");
                    payment.setStatus(5);
                    payment.setFinalResponse("999");
                    payment.setFinalResponseMessage(reqBody.getString("PaymentDescription"));
                    payment.setFinalResponseDate(Timestamp.from(Instant.now()));
                    palPaymentRepository.save(payment);
                }
            }, () -> {
                responseBody.put("statusCode", ResponseCodes.TRANSACTION_FAILED)
                        .put("statusDescription", "failed")
                        .put("statusMessage", "Request failed");
            });
        } catch (Exception ex) {
            responseBody.put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", ex.getMessage())
                    .put("statusMessage", ex.getMessage());
            log.log(Level.SEVERE, ex.getMessage());
        }
        return ResponseEntity.ok(responseBody.toString());
    }


}
