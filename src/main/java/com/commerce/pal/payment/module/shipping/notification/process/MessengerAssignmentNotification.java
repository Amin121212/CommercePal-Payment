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
                                });
                    });
            req.put("MessengerId", itemDelivery.getMessengerId());
            req.put("MerchantId", itemDelivery.getMerchantId());
            req.put("CustomerId", itemDelivery.getCustomerId());
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

            JSONObject emailPayload = new JSONObject();
            emailPayload.put("EmailDestination", mesRes.getString("email"));
            emailPayload.put("EmailSubject", getDeliveryType(payload.getString("DeliveryType")) + "-" + payload.getString("OrderRef"));
            emailPayload.put("EmailMessage", getDeliveryType(payload.getString("DeliveryType")) + "-" + payload.getString("OrderRef"));

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
                        emailPayload.put("EmailMessage", getDeliveryType(payload.getString("DeliveryType")) + "-" + payload.getString("OrderRef"));
                        globalMethods.processEmailWithoutTemplate(emailPayload);

                        if (payload.getString("SaleType").equals("M2C")) {
                            JSONObject cusReq = new JSONObject();
                            cusReq.put("Type", "CUSTOMER");
                            cusReq.put("TypeId", payload.getLong("CustomerId"));
                            JSONObject cusRes = dataAccessService.pickAndProcess(cusReq);
                            emailPayload.put("EmailDestination", cusRes.getString("email"));
                            emailPayload.put("EmailMessage", getDeliveryType(payload.getString("DeliveryType")) + "-" + payload.getString("OrderRef"));
                            globalMethods.processEmailWithoutTemplate(emailPayload);
                        } else {
                            JSONObject cusReq = new JSONObject();
                            cusReq.put("Type", "BUSINESS");
                            cusReq.put("TypeId", payload.getLong("CustomerId"));
                            JSONObject cusRes = dataAccessService.pickAndProcess(cusReq);
                            emailPayload.put("EmailDestination", cusRes.getString("email"));
                            emailPayload.put("EmailMessage", getDeliveryType(payload.getString("DeliveryType")) + "-" + payload.getString("OrderRef"));
                            globalMethods.processEmailWithoutTemplate(emailPayload);
                        }

                    } catch (Exception ex) {

                    }
                    break;
                case "MW":
                    try {

                        JSONObject merReq = new JSONObject();
                        merReq.put("Type", "MERCHANT");
                        merReq.put("TypeId", payload.getLong("MerchantId"));
                        JSONObject merRes = dataAccessService.pickAndProcess(merReq);
                        emailPayload.put("EmailMessage", getDeliveryType(payload.getString("DeliveryType")) + "-" + payload.getString("OrderRef"));
                        emailPayload.put("EmailDestination", merRes.getString("email"));
                        globalMethods.processEmailWithoutTemplate(emailPayload);

                    } catch (Exception ex) {
                    }
                    break;
                case "WC":
                    try {
                        if (payload.getString("SaleType").equals("M2C")) {
                            JSONObject cusReq = new JSONObject();
                            cusReq.put("Type", "CUSTOMER");
                            cusReq.put("TypeId", payload.getLong("CustomerId"));
                            JSONObject cusRes = dataAccessService.pickAndProcess(cusReq);
                            emailPayload.put("EmailDestination", cusRes.getString("email"));
                            emailPayload.put("EmailMessage", getDeliveryType(payload.getString("DeliveryType")) + "-" + payload.getString("OrderRef"));
                            globalMethods.processEmailWithoutTemplate(emailPayload);
                        } else {
                            JSONObject cusReq = new JSONObject();
                            cusReq.put("Type", "BUSINESS");
                            cusReq.put("TypeId", payload.getLong("CustomerId"));
                            JSONObject cusRes = dataAccessService.pickAndProcess(cusReq);
                            emailPayload.put("EmailDestination", cusRes.getString("email"));
                            emailPayload.put("EmailMessage", getDeliveryType(payload.getString("DeliveryType")) + "-" + payload.getString("OrderRef"));
                            globalMethods.processEmailWithoutTemplate(emailPayload);
                        }
                    } catch (Exception ex) {

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
