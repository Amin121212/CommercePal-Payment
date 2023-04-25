package com.commerce.pal.payment.module.shipping.notification.process;

import com.commerce.pal.payment.model.shipping.ItemMessengerDelivery;
import com.commerce.pal.payment.module.DataAccessService;
import com.commerce.pal.payment.repo.LoginValidationRepository;
import com.commerce.pal.payment.repo.payment.OrderItemRepository;
import com.commerce.pal.payment.repo.payment.OrderRepository;
import com.commerce.pal.payment.util.GlobalMethods;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Level;

@Log
@Service
@SuppressWarnings("Duplicates")
public class MessengerAssignmentNotification {
    private final GlobalMethods globalMethods;
    private final OrderRepository orderRepository;
    private final DataAccessService dataAccessService;
    private final OrderItemRepository orderItemRepository;
    private final LoginValidationRepository loginValidationRepository;

    @Autowired
    public MessengerAssignmentNotification(GlobalMethods globalMethods,
                                           OrderRepository orderRepository,
                                           DataAccessService dataAccessService,
                                           OrderItemRepository orderItemRepository,
                                           LoginValidationRepository loginValidationRepository) {
        this.globalMethods = globalMethods;
        this.orderRepository = orderRepository;
        this.dataAccessService = dataAccessService;
        this.orderItemRepository = orderItemRepository;
        this.loginValidationRepository = loginValidationRepository;
    }

    public void pickAndProcess(ItemMessengerDelivery itemDelivery, JSONObject req) {
        try {
            orderItemRepository.findById(itemDelivery.getOrderItemId())
                    .ifPresent(orderItem -> {
                        req.put("SubOrderRef", orderItem.getSubOrderNumber());
                        orderRepository.findById(orderItem.getOrderId())
                                .ifPresent(order -> {
                                    req.put("OrderRef", order.getOrderRef());
                                    req.put("SaleType", order.getSaleType());
                                    req.put("CustomerId", order.getCustomerId());
                                    req.put("BusinessId", order.getBusinessId());

                                    JSONObject prodReq = new JSONObject();
                                    prodReq.put("Type", "PRODUCT-AND-SUB");
                                    prodReq.put("TypeId", orderItem.getProductLinkingId());
                                    prodReq.put("SubProductId", orderItem.getSubProductId());
                                    JSONObject productBdy = dataAccessService.pickAndProcess(prodReq);

                                    req.put("OrderRef", order.getOrderRef());
                                    req.put("OrderDate", order.getOrderDate());
                                    req.put("ProductName", productBdy.getString("productName"));
                                    req.put("ProductImage", productBdy.getString("webImage"));
                                    req.put("NoOfProduct", orderItem.getQuantity());
                                    req.put("ItemOrderRef", orderItem.getSubOrderNumber());
                                    req.put("StatusDate", Timestamp.from(Instant.now()));
                                });
                    });
            req.put("MessengerId", itemDelivery.getMessengerId());
            req.put("MerchantId", itemDelivery.getMerchantId());
            req.put("WareHouse", itemDelivery.getWareHouseId());
            req.put("DeliveryType", itemDelivery.getDeliveryType());
            messengerDeliveryNotification(req);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
        }
    }

    public void messengerDeliveryNotification(JSONObject payload) {
        try {
            JSONObject mesReq = new JSONObject();
            mesReq.put("Type", "MESSENGER");
            mesReq.put("TypeId", payload.getLong("MessengerId"));
            JSONObject mesRes = dataAccessService.pickAndProcess(mesReq);

            JSONObject emailPayload = new JSONObject(payload);
            emailPayload.put("EmailDestination", mesRes.getString("email"));
            emailPayload.put("EmailSubject", getDeliveryType(payload.getString("DeliveryType")) + "-" + payload.getString("OrderRef"));
            emailPayload.put("EmailMessage", getDeliveryType(payload.getString("DeliveryType")) + "-" + payload.getString("OrderRef"));

            JSONObject slackBody = new JSONObject();
            slackBody.put("TemplateId", "15");
            slackBody.put("messenger_name", mesRes.getString("firstName"));
            slackBody.put("phone_number", mesRes.getString("ownerPhoneNumber"));
            slackBody.put("sub_ref", payload.getString("SubOrderRef"));
            globalMethods.sendSlackNotification(slackBody);

            loginValidationRepository.findLoginValidationByEmailAddress(mesRes.getString("email"))
                    .ifPresent(user -> {
                        JSONObject pushPayload = new JSONObject();
                        pushPayload.put("UserId", user.getUserOneSignalId() != null ? user.getUserOneSignalId() : "5c66ca50-c009-480f-a200-72c244d74ff4");
                        pushPayload.put("Header", "Assigned Item : " + payload.getString("OrderRef"));
                        pushPayload.put("Message", getDeliveryType(payload.getString("DeliveryType")) + "-" + payload.getString("OrderRef"));
                        JSONObject data = new JSONObject();
                        data.put("OrderRef", payload.getString("OrderRef"));
                        pushPayload.put("data", data);
                        globalMethods.sendPushNotification(pushPayload);
                    });
            globalMethods.processEmailWithoutTemplate(emailPayload);
            switch (payload.getString("DeliveryType")) {
                case "MC":
                    try {
                        JSONObject merReq = new JSONObject();
                        merReq.put("Type", "MERCHANT");
                        merReq.put("TypeId", payload.getLong("MerchantId"));
                        JSONObject merRes = dataAccessService.pickAndProcess(merReq);
                        emailPayload.put("EmailDestination", merRes.getString("email"));
                        emailPayload.put("EmailSubject", "Item Assigned for Pick-Up : " + payload.getString("OrderRef"));
                        emailPayload.put("EmailMessage", getDeliveryType(payload.getString("DeliveryType")) + "-" + payload.getString("OrderRef"));
                        globalMethods.processEmailWithoutTemplate(emailPayload);

                        loginValidationRepository.findLoginValidationByEmailAddress(merRes.getString("email"))
                                .ifPresent(user -> {
                                    JSONObject pushPayload = new JSONObject();
                                    pushPayload.put("UserId", user.getUserOneSignalId() != null ? user.getUserOneSignalId() : "5c66ca50-c009-480f-a200-72c244d74ff4");
                                    pushPayload.put("Header", "Item Assigned for Pick-Up : " + payload.getString("OrderRef"));
                                    pushPayload.put("Message", getDeliveryType(payload.getString("DeliveryType")) + "-" + payload.getString("OrderRef"));
                                    JSONObject data = new JSONObject();
                                    data.put("OrderRef", payload.getString("OrderRef"));
                                    pushPayload.put("data", data);
                                    globalMethods.sendPushNotification(pushPayload);
                                });

                        if (payload.getString("SaleType").equals("M2C")) {
                            JSONObject cusReq = new JSONObject();
                            cusReq.put("Type", "CUSTOMER");
                            cusReq.put("TypeId", payload.getLong("CustomerId"));
                            JSONObject cusRes = dataAccessService.pickAndProcess(cusReq);
                            emailPayload.put("EmailDestination", cusRes.getString("email"));
                            emailPayload.put("name", cusRes.getString("firstName"));
                        } else {
                            JSONObject cusReq = new JSONObject();
                            cusReq.put("Type", "BUSINESS");
                            cusReq.put("TypeId", payload.getLong("BusinessId"));
                            JSONObject cusRes = dataAccessService.pickAndProcess(cusReq);
                            emailPayload.put("EmailDestination", cusRes.getString("email"));
                            emailPayload.put("name", cusRes.getString("firstName"));
                        }

                        emailPayload.put("ShippingStatus", "Messenger Assigned for Item PickUp from the Merchant to Customer");
                        emailPayload.put("HasTemplate", "YES");
                        emailPayload.put("TemplateName", "customer-tracking");
                        emailPayload.put("EmailMessage", "Order Payment");
                        globalMethods.sendEmailNotification(emailPayload);

                    } catch (Exception ex) {
                        log.log(Level.WARNING, "MC ERROR " + ex.getMessage());
                    }
                    break;
                case "MW":
                    try {
                        JSONObject merReq = new JSONObject();
                        merReq.put("Type", "MERCHANT");
                        merReq.put("TypeId", payload.getLong("MerchantId"));
                        JSONObject merRes = dataAccessService.pickAndProcess(merReq);
                        emailPayload.put("EmailSubject", "Item Assigned for Pick-Up : " + payload.getString("OrderRef"));
                        emailPayload.put("EmailMessage", getDeliveryType(payload.getString("DeliveryType")) + "-" + payload.getString("OrderRef"));
                        globalMethods.processEmailWithoutTemplate(emailPayload);

                        loginValidationRepository.findLoginValidationByEmailAddress(merRes.getString("email"))
                                .ifPresent(user -> {
                                    JSONObject pushPayload = new JSONObject();
                                    pushPayload.put("UserId", user.getUserOneSignalId() != null ? user.getUserOneSignalId() : "5c66ca50-c009-480f-a200-72c244d74ff4");
                                    pushPayload.put("Header", "Item Assigned for Pick-Up : " + payload.getString("OrderRef"));
                                    pushPayload.put("Message", getDeliveryType(payload.getString("DeliveryType")) + "-" + payload.getString("OrderRef"));
                                    JSONObject data = new JSONObject();
                                    data.put("OrderRef", payload.getString("OrderRef"));
                                    pushPayload.put("data", data);
                                    globalMethods.sendPushNotification(pushPayload);
                                });

                    } catch (Exception ex) {
                        log.log(Level.WARNING, "MW ERROR " + ex.getMessage());
                    }
                    break;
                case "WC":
                    try {
                        emailPayload.put("EmailSubject", "Item Assigned for Pick-Up : " + payload.getString("OrderRef"));
                        if (payload.getString("SaleType").equals("M2C")) {
                            JSONObject cusReq = new JSONObject();
                            cusReq.put("Type", "CUSTOMER");
                            cusReq.put("TypeId", payload.getLong("CustomerId"));
                            JSONObject cusRes = dataAccessService.pickAndProcess(cusReq);
                            emailPayload.put("EmailDestination", cusRes.getString("email"));
                            emailPayload.put("name", cusRes.getString("firstName"));
                        } else {
                            JSONObject cusReq = new JSONObject();
                            cusReq.put("Type", "BUSINESS");
                            cusReq.put("TypeId", payload.getLong("BusinessId"));
                            JSONObject cusRes = dataAccessService.pickAndProcess(cusReq);
                            emailPayload.put("EmailDestination", cusRes.getString("email"));
                            emailPayload.put("name", cusRes.getString("firstName"));
                        }

                        emailPayload.put("ShippingStatus", "Messenger Assigned for Item PickUp from the WareHouse to Customer");
                        emailPayload.put("HasTemplate", "YES");
                        emailPayload.put("TemplateName", "customer-tracking");
                        emailPayload.put("EmailMessage", "Order Payment");
                        globalMethods.sendEmailNotification(emailPayload);

                    } catch (Exception ex) {
                        log.log(Level.WARNING, "WC ERROR " + ex.getMessage());
                    }
                    break;
            }

        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
        }
    }

    public void customerDeliveryNotification(JSONObject payload) {

    }

    public void merchantPickUpNotification(JSONObject payload) {

    }

    public String getDeliveryType(String type) {
        String message = "";
        switch (type) {
            case "MC":
                message = "Merchant To Customer";
                break;
            case "MW":
                message = "Merchant To WareHouse";
                break;
            case "WC":
                message = "WareHouse To Customer";
                break;
        }

        return message;
    }
}
