package com.commerce.pal.payment.module.shipping.notification.process;

import com.commerce.pal.payment.module.DataAccessService;
import com.commerce.pal.payment.repo.payment.OrderItemRepository;
import com.commerce.pal.payment.repo.payment.OrderRepository;
import com.commerce.pal.payment.util.GlobalMethods;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Log
@Service
@SuppressWarnings("Duplicates")
public class MerchantAcceptAndPickUpNotification {

    private final GlobalMethods globalMethods;
    private final OrderRepository orderRepository;
    private final DataAccessService dataAccessService;
    private final OrderItemRepository orderItemRepository;

    public MerchantAcceptAndPickUpNotification(GlobalMethods globalMethods,
                                               OrderRepository orderRepository,
                                               DataAccessService dataAccessService,
                                               OrderItemRepository orderItemRepository) {
        this.globalMethods = globalMethods;
        this.orderRepository = orderRepository;
        this.dataAccessService = dataAccessService;
        this.orderItemRepository = orderItemRepository;
    }

    public void pickAndProcess(Long itemId) {
        try {
            orderItemRepository.findById(itemId)
                    .ifPresent(orderItem -> {
                        orderRepository.findById(orderItem.getOrderId())
                                .ifPresent(order -> {
                                    JSONObject emailPayload = new JSONObject();
                                    if (order.getSaleType().equals("M2C")) {
                                        JSONObject cusReq = new JSONObject();
                                        cusReq.put("Type", "CUSTOMER");
                                        cusReq.put("TypeId", order.getCustomerId());
                                        JSONObject cusRes = dataAccessService.pickAndProcess(cusReq);
                                        emailPayload.put("name", cusRes.getString("firstName"));
                                        emailPayload.put("EmailDestination", cusRes.getString("email"));
                                    } else {
                                        JSONObject cusReq = new JSONObject();
                                        cusReq.put("Type", "BUSINESS");
                                        cusReq.put("TypeId", order.getBusinessId());
                                        JSONObject cusRes = dataAccessService.pickAndProcess(cusReq);
                                        emailPayload.put("name", cusRes.getString("firstName"));
                                        emailPayload.put("EmailDestination", cusRes.getString("email"));
                                    }


                                    // Send Merchant Status Notification
                                    JSONObject prodReq = new JSONObject();
                                    prodReq.put("Type", "PRODUCT-AND-SUB");
                                    prodReq.put("TypeId", orderItem.getProductLinkingId());
                                    prodReq.put("SubProductId", orderItem.getSubProductId());
                                    JSONObject productBdy = dataAccessService.pickAndProcess(prodReq);

                                    emailPayload.put("OrderRef", order.getOrderRef());
                                    emailPayload.put("OrderDate", order.getOrderDate());
                                    emailPayload.put("ProductName", productBdy.getString("ProductName"));
                                    emailPayload.put("ProductImage", productBdy.getString("webImage"));
                                    emailPayload.put("NoOfProduct", orderItem.getQuantity());
                                    emailPayload.put("ItemOrderRef", orderItem.getSubOrderNumber());
                                    emailPayload.put("StatusDate", Timestamp.from(Instant.now()));
                                    emailPayload.put("ShippingStatus", "Merchant has accepted your Item Order,Messenger will pick it up");
                                    emailPayload.put("HasTemplate", "YES");
                                    emailPayload.put("TemplateName", "customer-tracking");
                                    emailPayload.put("orderRef", order.getOrderRef());
                                    emailPayload.put("EmailSubject", "MERCHANT ITEM ACCEPTANCE - " + order.getOrderRef());
                                    emailPayload.put("EmailMessage", "Order Payment");
                                    globalMethods.sendEmailNotification(emailPayload);

                                    JSONObject merReq = new JSONObject();
                                    merReq.put("Type", "MERCHANT");
                                    merReq.put("TypeId", orderItem.getMerchantId());
                                    //JSONObject merRes = dataAccessService.pickAndProcess(merReq);
                                });
                    });

        } catch (Exception ex) {

        }
    }
}
