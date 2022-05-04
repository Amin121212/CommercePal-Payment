package com.commerce.pal.payment.module.shipping.notification.merchant;

import com.commerce.pal.payment.integ.notification.EmailClient;
import com.commerce.pal.payment.module.DataAccessService;
import com.commerce.pal.payment.repo.payment.OrderItemRepository;
import com.commerce.pal.payment.repo.payment.OrderRepository;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Log
@Service
@SuppressWarnings("Duplicates")
public class MerchantCustomerNotificationService {
    @Autowired
    private EmailClient emailClient;

    private final OrderRepository orderRepository;
    private final DataAccessService dataAccessService;
    private final OrderItemRepository orderItemRepository;

    @Autowired
    public MerchantCustomerNotificationService(OrderRepository orderRepository,
                                               DataAccessService dataAccessService,
                                               OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.dataAccessService = dataAccessService;
        this.orderItemRepository = orderItemRepository;
    }

    public void processNewOrderProcess(String orderRef) {
        try {
            orderRepository.findOrderByOrderRef(orderRef)
                    .ifPresentOrElse(order -> {
                        JSONObject cusReq = new JSONObject();
                        cusReq.put("Type", "CUSTOMER");
                        cusReq.put("TypeId", order.getCustomerId());
                        JSONObject cusRes = dataAccessService.pickAndProcess(cusReq);

                        JSONObject orderPay = new JSONObject();
                        orderPay.put("CustomerName", cusRes.getString("firstName"));
                        orderPay.put("OrderRef", order.getOrderRef());
                        orderPay.put("OrderDate", order.getOrderDate());
                        List<JSONObject> customerOrderItems = new ArrayList<>();
                        orderItemRepository.findAllByOrderId(order.getOrderId())
                                .forEach(merchant -> {
                                    JSONObject merReq = new JSONObject();
                                    merReq.put("Type", "MERCHANT");
                                    merReq.put("TypeId", merchant);
                                    JSONObject merRes = dataAccessService.pickAndProcess(merReq);
                                    List<JSONObject> orderItems = new ArrayList<>();
                                    orderItemRepository.findOrderItemsByOrderIdAndMerchantId(order.getOrderId(), merchant)
                                            .forEach(orderItem -> {
                                                JSONObject itemPay = new JSONObject();
                                                JSONObject prodReq = new JSONObject();
                                                prodReq.put("Type", "PRODUCT");
                                                prodReq.put("TypeId", orderItem.getProductLinkingId());
                                                JSONObject productBdy = dataAccessService.pickAndProcess(prodReq);
                                                itemPay.put("ProductName", productBdy.getString("ProductName"));
                                                itemPay.put("ProductImage", productBdy.getString("webImage"));
                                                itemPay.put("NoOfProduct", orderItem.getQuantity());
                                                itemPay.put("ItemOrderRef", orderItem.getSubOrderNumber());
                                                orderItems.add(itemPay);
                                                customerOrderItems.add(itemPay);
                                            });
                                    orderPay.put("orderItems", orderItems);

                                    orderPay.put("email", merRes.getString("email"));
                                    orderPay.put("subject", "New Order Ref : " + order.getOrderRef());
                                    orderPay.put("templates", "merchant-new-order.ftl");
                                    //Send to Merchant
                                    emailClient.emailTemplateSender(orderPay);

                                });
                        orderPay.put("orderItems", customerOrderItems);
                        orderPay.put("email", cusRes.getString("email"));
                        orderPay.put("subject", "New Order Ref : " + order.getOrderRef());
                        orderPay.put("templates", "merchant-new-order.ftl");
                        //Send to Customer
                        emailClient.emailTemplateSender(orderPay);
                    }, () -> {
                        log.log(Level.WARNING, "Invalid Order Ref Passed : " + orderRef);
                    });
        } catch (Exception ex) {

        }
    }
}
