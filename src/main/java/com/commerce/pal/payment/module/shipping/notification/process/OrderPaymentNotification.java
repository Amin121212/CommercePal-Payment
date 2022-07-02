package com.commerce.pal.payment.module.shipping.notification.process;

import com.commerce.pal.payment.model.shipping.ItemShipmentStatus;
import com.commerce.pal.payment.module.DataAccessService;
import com.commerce.pal.payment.repo.LoginValidationRepository;
import com.commerce.pal.payment.repo.payment.OrderItemRepository;
import com.commerce.pal.payment.repo.payment.OrderRepository;
import com.commerce.pal.payment.repo.shipping.ItemShipmentStatusRepository;
import com.commerce.pal.payment.util.GlobalMethods;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static com.commerce.pal.payment.util.StatusCodes.NewOrder;

@Log
@Service
@SuppressWarnings("Duplicates")
public class OrderPaymentNotification {

    private final GlobalMethods globalMethods;
    private final OrderRepository orderRepository;
    private final DataAccessService dataAccessService;
    private final OrderItemRepository orderItemRepository;
    private final LoginValidationRepository loginValidationRepository;
    private final ItemShipmentStatusRepository itemShipmentStatusRepository;


    public OrderPaymentNotification(GlobalMethods globalMethods,
                                    OrderRepository orderRepository,
                                    DataAccessService dataAccessService,
                                    OrderItemRepository orderItemRepository,
                                    LoginValidationRepository loginValidationRepository,
                                    ItemShipmentStatusRepository itemShipmentStatusRepository) {
        this.globalMethods = globalMethods;
        this.orderRepository = orderRepository;
        this.dataAccessService = dataAccessService;
        this.orderItemRepository = orderItemRepository;
        this.loginValidationRepository = loginValidationRepository;
        this.itemShipmentStatusRepository = itemShipmentStatusRepository;
    }

    public void pickAndProcess(String orderRef) {
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
                                                JSONObject subProdReq = new JSONObject();
                                                subProdReq.put("Type", "SUB-PRODUCT");
                                                subProdReq.put("TypeId", orderItem.getSubProductId());
                                                JSONObject subProductBdy = dataAccessService.pickAndProcess(subProdReq);

                                                orderItems.add(itemPay);
                                                customerOrderItems.add(itemPay);

                                                ItemShipmentStatus itemShipmentStatus = new ItemShipmentStatus();
                                                itemShipmentStatus.setItemId(orderItem.getItemId());
                                                itemShipmentStatus.setShipmentStatus(NewOrder);
                                                itemShipmentStatus.setComments("New Order Payment and Notifications");
                                                itemShipmentStatus.setStatus(1);
                                                itemShipmentStatus.setCreatedDate(Timestamp.from(Instant.now()));
                                                itemShipmentStatusRepository.save(itemShipmentStatus);
                                            });
                                    orderPay.put("orderItems", orderItems);

                                    if (!merchant.equals(0)) {
                                        JSONObject merReq = new JSONObject();
                                        merReq.put("Type", "MERCHANT");
                                        merReq.put("TypeId", merchant);
                                        JSONObject merRes = dataAccessService.pickAndProcess(merReq);


                                        loginValidationRepository.findLoginValidationByEmailAddress(merRes.getString("email"))
                                                .ifPresent(user -> {
                                                    JSONObject pushPayload = new JSONObject();
                                                    pushPayload.put("UserId", user.getUserOneSignalId() != null ? user.getUserOneSignalId() : "5c66ca50-c009-480f-a200-72c244d74ff4");
                                                    pushPayload.put("Header", "New Order : " + order.getOrderRef());
                                                    pushPayload.put("Message", "New Order : " + order.getOrderRef());
                                                    JSONObject data = new JSONObject();
                                                    data.put("OrderRef", order.getOrderRef());
                                                    pushPayload.put("data", data);
                                                    globalMethods.sendPushNotification(pushPayload);
                                                });

                                        orderPay.put("email", merRes.getString("email"));
                                        orderPay.put("subject", "New Order Ref : " + order.getOrderRef() + " (Merchant)");
                                        orderPay.put("templates", "merchant-new-order.ftl");
                                        //Send to Merchant
                                        globalMethods.processEmailWithTemplate(orderPay);
                                    } else { // DO for WareHouse Id

                                    }
                                });
                        orderPay.put("orderItems", customerOrderItems);
                        orderPay.put("email", cusRes.getString("email"));
                        orderPay.put("subject", "New Order Ref : " + order.getOrderRef() + " (Customer)");
                        orderPay.put("templates", "merchant-new-order.ftl");
                        //Send to Customer
                        globalMethods.processEmailWithTemplate(orderPay);
                    }, () -> {
                        log.log(Level.WARNING, "Invalid Order Ref Passed : " + orderRef);
                    });
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
        }
    }
}
