package com.commerce.pal.payment.controller.portal.warehouse;

import com.commerce.pal.payment.model.shipping.ItemMessengerDelivery;
import com.commerce.pal.payment.model.shipping.ItemShipmentStatus;
import com.commerce.pal.payment.module.shipping.notification.process.MessengerAssignmentNotification;
import com.commerce.pal.payment.repo.payment.OrderItemRepository;
import com.commerce.pal.payment.repo.payment.OrderRepository;
import com.commerce.pal.payment.repo.shipping.ItemMessengerDeliveryRepository;
import com.commerce.pal.payment.repo.shipping.ItemShipmentStatusRepository;
import com.commerce.pal.payment.util.GlobalMethods;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Level;

import static com.commerce.pal.payment.util.StatusCodes.AssignMessengerPickAtMerchant;

@Log
@CrossOrigin(origins = {"*"}, maxAge = 3600L)
@RestController
@RequestMapping({"/prime/api/v1/ware-house/shipping"})
@SuppressWarnings("Duplicates")
public class WareHouseShippingController {
    private final GlobalMethods globalMethods;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemShipmentStatusRepository itemShipmentStatusRepository;
    private final MessengerAssignmentNotification messengerAssignmentNotification;
    private final ItemMessengerDeliveryRepository itemMessengerDeliveryRepository;

    @Autowired
    public WareHouseShippingController(GlobalMethods globalMethods,
                                       OrderRepository orderRepository,
                                       OrderItemRepository orderItemRepository,
                                       ItemShipmentStatusRepository itemShipmentStatusRepository,
                                       MessengerAssignmentNotification messengerAssignmentNotification,
                                       ItemMessengerDeliveryRepository itemMessengerDeliveryRepository) {
        this.globalMethods = globalMethods;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.itemShipmentStatusRepository = itemShipmentStatusRepository;
        this.messengerAssignmentNotification = messengerAssignmentNotification;
        this.itemMessengerDeliveryRepository = itemMessengerDeliveryRepository;
    }

    // Assign the Items to Specific WareHouse
    // Filter Paid orders by status
    @RequestMapping(value = "/assign-messenger-to-delivery", method = RequestMethod.POST)
    public ResponseEntity<?> assignMessengerToDelivery(@RequestBody String req) {
        JSONObject responseMap = new JSONObject();
        try {
            JSONObject request = new JSONObject(req);
            orderItemRepository.findById(request.getLong("OrderItemId"))
                    .ifPresentOrElse(orderItem -> {
                        log.log(Level.INFO, orderRepository.findByOrderId(orderItem.getOrderId()).getCustomerId().toString());
                        String validationCode = globalMethods.generateValidationCode();
                        String deliveryCode = globalMethods.generateValidationCode();

                        JSONObject delivery = new JSONObject();
                        delivery.put("PickingCode", validationCode);
                        delivery.put("DeliveryCode", deliveryCode);

                        orderItem.setShipmentStatus(AssignMessengerPickAtMerchant);
                        orderItem.setShipmentUpdateDate(Timestamp.from(Instant.now()));

                        itemShipmentStatusRepository.findItemShipmentStatusByItemIdAndShipmentStatus(
                                        orderItem.getItemId(), AssignMessengerPickAtMerchant)
                                .ifPresentOrElse(itemShipmentStatus -> {
                                    itemShipmentStatus.setComments(request.getString("Comments"));
                                    itemShipmentStatus.setStatus(1);
                                    itemShipmentStatus.setCreatedDate(Timestamp.from(Instant.now()));
                                    itemShipmentStatusRepository.save(itemShipmentStatus);
                                }, () -> {
                                    ItemShipmentStatus itemShipmentStatus = new ItemShipmentStatus();
                                    itemShipmentStatus.setItemId(orderItem.getItemId());
                                    itemShipmentStatus.setShipmentStatus(AssignMessengerPickAtMerchant);
                                    itemShipmentStatus.setComments(request.getString("Comments"));
                                    itemShipmentStatus.setStatus(1);
                                    itemShipmentStatus.setCreatedDate(Timestamp.from(Instant.now()));
                                    itemShipmentStatusRepository.save(itemShipmentStatus);
                                });

                        itemMessengerDeliveryRepository.findItemMessengerDeliveryByOrderItemIdAndDeliveryTypeAndValidationStatus(
                                orderItem.getItemId(), request.getString("DeliveryType"), 0
                        ).ifPresentOrElse(itemMessengerDelivery -> {
                            itemMessengerDelivery.setDeliveryType(request.getString("DeliveryType"));
                            switch (request.getString("DeliveryType")) {
                                case "MC":
                                    itemMessengerDelivery.setMerchantId(orderItem.getMerchantId());
                                    itemMessengerDelivery.setCustomerId(
                                            orderRepository.findByOrderId(orderItem.getOrderId()).getSaleType().equals("M2C") ?
                                                    orderRepository.findByOrderId(orderItem.getOrderId()).getCustomerId() :
                                                    orderRepository.findByOrderId(orderItem.getOrderId()).getBusinessId()
                                    );
                                    break;
                                case "MW":
                                    itemMessengerDelivery.setMerchantId(orderItem.getMerchantId());
                                    itemMessengerDelivery.setWareHouseId(Long.valueOf(orderItem.getAssignedWareHouseId()));
                                    break;
                                case "WC":
                                    itemMessengerDelivery.setWareHouseId(Long.valueOf(orderItem.getAssignedWareHouseId()));
                                    itemMessengerDelivery.setCustomerId(
                                            orderRepository.findByOrderId(orderItem.getOrderId()).getSaleType().equals("M2C") ?
                                                    orderRepository.findByOrderId(orderItem.getOrderId()).getCustomerId() :
                                                    orderRepository.findByOrderId(orderItem.getOrderId()).getBusinessId()
                                    );
                                    break;
                            }
                            itemMessengerDelivery.setValidationCode(globalMethods.encryptCode(validationCode));
                            itemMessengerDelivery.setValidationStatus(0);
                            itemMessengerDelivery.setDeliveryCode(globalMethods.encryptCode(deliveryCode));
                            itemMessengerDelivery.setDeliveryStatus(0);
                            itemMessengerDelivery.setCreatedDate(Timestamp.from(Instant.now()));
                            itemMessengerDeliveryRepository.save(itemMessengerDelivery);

                            // Send Notification to Respective Users
                            messengerAssignmentNotification.pickAndProcess(itemMessengerDelivery, delivery);
                        }, () -> {
                            ItemMessengerDelivery itemMessengerDelivery = new ItemMessengerDelivery();
                            itemMessengerDelivery.setOrderItemId(orderItem.getItemId());
                            itemMessengerDelivery.setDeliveryType(request.getString("DeliveryType"));
                            itemMessengerDelivery.setMessengerId(request.getLong("MessengerId"));
                            itemMessengerDelivery.setMerchantId(0l);
                            itemMessengerDelivery.setWareHouseId(0l);
                            itemMessengerDelivery.setCustomerId(0l);
                            switch (request.getString("DeliveryType")) {
                                case "MC":
                                    itemMessengerDelivery.setMerchantId(orderItem.getMerchantId());
                                    itemMessengerDelivery.setCustomerId(
                                            orderRepository.findByOrderId(orderItem.getOrderId()).getSaleType().equals("M2C") ?
                                                    orderRepository.findByOrderId(orderItem.getOrderId()).getCustomerId() :
                                                    orderRepository.findByOrderId(orderItem.getOrderId()).getBusinessId()
                                    );
                                    break;
                                case "MW":
                                    itemMessengerDelivery.setMerchantId(orderItem.getMerchantId());
                                    itemMessengerDelivery.setWareHouseId(Long.valueOf(orderItem.getAssignedWareHouseId()));
                                    break;
                                case "WC":
                                    itemMessengerDelivery.setWareHouseId(Long.valueOf(orderItem.getAssignedWareHouseId()));
                                    itemMessengerDelivery.setCustomerId(
                                            orderRepository.findByOrderId(orderItem.getOrderId()).getSaleType().equals("M2C") ?
                                                    orderRepository.findByOrderId(orderItem.getOrderId()).getCustomerId() :
                                                    orderRepository.findByOrderId(orderItem.getOrderId()).getBusinessId()
                                    );
                                    break;
                            }

                            itemMessengerDelivery.setValidationCode(globalMethods.encryptCode(validationCode));
                            itemMessengerDelivery.setValidationStatus(0);
                            itemMessengerDelivery.setDeliveryCode(globalMethods.encryptCode(deliveryCode));
                            itemMessengerDelivery.setDeliveryStatus(0);
                            itemMessengerDelivery.setStatus(0);
                            itemMessengerDelivery.setCreatedDate(Timestamp.from(Instant.now()));
                            itemMessengerDeliveryRepository.save(itemMessengerDelivery);

                            // Send Notification to Respective Users
                            messengerAssignmentNotification.pickAndProcess(itemMessengerDelivery, delivery);
                        });


                        responseMap.put("statusCode", ResponseCodes.SUCCESS)
                                .put("statusDescription", "Success")
                                .put("statusMessage", "Success");
                    }, () -> {
                        responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                                .put("statusDescription", "Merchant Does not exists")
                                .put("statusMessage", "Merchant Does not exists");
                    });

        } catch (Exception ex) {
            responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                    .put("statusDescription", "Merchant Does not exists")
                    .put("statusMessage", "Merchant Does not exists");
            log.log(Level.WARNING, ex.getMessage());
        }
        return ResponseEntity.ok(responseMap.toString());
    }

}
