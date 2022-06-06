package com.commerce.pal.payment.controller.shipping.user;

import com.commerce.pal.payment.model.shipping.ItemShipmentStatus;
import com.commerce.pal.payment.module.DataAccessService;
import com.commerce.pal.payment.module.ValidateAccessToken;
import com.commerce.pal.payment.module.shipping.notification.process.MerchantAcceptAndPickUpNotification;
import com.commerce.pal.payment.repo.payment.OrderItemRepository;
import com.commerce.pal.payment.repo.payment.OrderRepository;
import com.commerce.pal.payment.repo.shipping.ItemMessengerDeliveryRepository;
import com.commerce.pal.payment.repo.shipping.ItemShipmentStatusRepository;
import com.commerce.pal.payment.repo.shipping.ShipmentStatusRepository;
import com.commerce.pal.payment.util.GlobalMethods;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static com.commerce.pal.payment.util.StatusCodes.*;

@Log
@CrossOrigin(origins = {"*"}, maxAge = 3600L)
@RestController
@RequestMapping({"/prime/api/v1/messenger/shipping"})
@SuppressWarnings("Duplicates")
public class MessengerShippingController {
    private final GlobalMethods globalMethods;
    private final OrderRepository orderRepository;
    private final DataAccessService dataAccessService;
    private final OrderItemRepository orderItemRepository;
    private final ValidateAccessToken validateAccessToken;
    private final ShipmentStatusRepository shipmentStatusRepository;
    private final ItemShipmentStatusRepository itemShipmentStatusRepository;
    private final ItemMessengerDeliveryRepository itemMessengerDeliveryRepository;
    private final MerchantAcceptAndPickUpNotification merchantAcceptAndPickUpNotification;

    @Autowired
    public MessengerShippingController(GlobalMethods globalMethods,
                                       OrderRepository orderRepository,
                                       DataAccessService dataAccessService,
                                       OrderItemRepository orderItemRepository,
                                       ValidateAccessToken validateAccessToken,
                                       ShipmentStatusRepository shipmentStatusRepository,
                                       ItemShipmentStatusRepository itemShipmentStatusRepository,
                                       ItemMessengerDeliveryRepository itemMessengerDeliveryRepository,
                                       MerchantAcceptAndPickUpNotification merchantAcceptAndPickUpNotification) {
        this.globalMethods = globalMethods;
        this.orderRepository = orderRepository;
        this.dataAccessService = dataAccessService;
        this.orderItemRepository = orderItemRepository;
        this.validateAccessToken = validateAccessToken;
        this.shipmentStatusRepository = shipmentStatusRepository;
        this.itemShipmentStatusRepository = itemShipmentStatusRepository;
        this.itemMessengerDeliveryRepository = itemMessengerDeliveryRepository;
        this.merchantAcceptAndPickUpNotification = merchantAcceptAndPickUpNotification;
    }


    @RequestMapping(value = {"/filter-delivery"}, method = {RequestMethod.GET}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> filterDeliveryStatus(@RequestHeader("Authorization") String accessToken,
                                                  @RequestParam("status") Integer status) {
        JSONObject responseMap = new JSONObject();
        JSONObject valTokenReq = new JSONObject();
        valTokenReq.put("AccessToken", accessToken)
                .put("UserType", "M");
        JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);
        if (valTokenBdy.getString("Status").equals("00")) {
            JSONObject userDetails = valTokenBdy.getJSONObject("UserDetails");
            JSONObject messengerInfo = userDetails.getJSONObject("messengerInfo");
            Long messengerId = Long.valueOf(messengerInfo.getInt("userId"));

            List<Integer> deliveryStatus = new ArrayList<>();
            if (status.equals(0)) {
                deliveryStatus.add(0);
                deliveryStatus.add(1);
                deliveryStatus.add(3);
            } else {
                deliveryStatus.add(status);
            }
            List<JSONObject> deliveryList = new ArrayList<>();

            itemMessengerDeliveryRepository.findItemMessengerDeliveriesByMessengerIdAndStatusIn(
                    messengerId, globalMethods.convertListToIntegerArray(deliveryStatus)
            ).forEach(itemMessengerDelivery -> {
                JSONObject delivery = new JSONObject();
                delivery.put("DeliveryType", itemMessengerDelivery.getDeliveryType());
                delivery.put("PickingDate", itemMessengerDelivery.getPickingDate());
                delivery.put("DeliveryId", itemMessengerDelivery.getId());
                delivery.put("ItemOrderId", itemMessengerDelivery.getOrderItemId());
                delivery.put("AssignedDate", itemMessengerDelivery.getCreatedDate());
                delivery.put("MerchantId", itemMessengerDelivery.getMerchantId());
                delivery.put("CustomerId", itemMessengerDelivery.getCustomerId());
                delivery.put("WareHouseId", itemMessengerDelivery.getWareHouseId());
                orderItemRepository.findById(itemMessengerDelivery.getOrderItemId())
                        .ifPresent(orderItem -> {
                            delivery.put("ItemOrderRef", orderItem.getSubOrderNumber());
                        });
                deliveryList.add(delivery);
            });
            responseMap.put("statusCode", ResponseCodes.SUCCESS)
                    .put("statusDescription", "success")
                    .put("deliveryList", deliveryList)
                    .put("statusMessage", "Request Successful");
        } else {
            responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                    .put("statusDescription", "Merchant Does not exists")
                    .put("statusMessage", "Merchant Does not exists");
        }
        return ResponseEntity.ok(responseMap.toString());
    }

    @RequestMapping(value = {"/delivery-item"}, method = {RequestMethod.GET}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> deliveryItem(@RequestHeader("Authorization") String accessToken,
                                          @RequestParam("DeliveryId") Long DeliveryId) {
        JSONObject responseMap = new JSONObject();
        JSONObject valTokenReq = new JSONObject();
        valTokenReq.put("AccessToken", accessToken)
                .put("UserType", "M");
        JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);
        if (valTokenBdy.getString("Status").equals("00")) {
            JSONObject userDetails = valTokenBdy.getJSONObject("UserDetails");
            JSONObject messengerInfo = userDetails.getJSONObject("messengerInfo");
            Long messengerId = Long.valueOf(messengerInfo.getInt("userId"));

            JSONObject delivery = new JSONObject();
            itemMessengerDeliveryRepository.findItemMessengerDeliveryByIdAndMessengerId(
                    messengerId, DeliveryId
            ).ifPresentOrElse(itemMessengerDelivery -> {
                delivery.put("DeliveryType", itemMessengerDelivery.getDeliveryType());
                delivery.put("PickingDate", itemMessengerDelivery.getPickingDate());
                delivery.put("DeliveryId", itemMessengerDelivery.getId());
                delivery.put("ItemOrderId", itemMessengerDelivery.getOrderItemId());
                delivery.put("AssignedDate", itemMessengerDelivery.getCreatedDate());
                delivery.put("MerchantId", itemMessengerDelivery.getMerchantId());
                delivery.put("CustomerId", itemMessengerDelivery.getCustomerId());
                delivery.put("WareHouseId", itemMessengerDelivery.getWareHouseId());
                orderItemRepository.findById(itemMessengerDelivery.getOrderItemId())
                        .ifPresent(orderItem -> {
                            delivery.put("ItemOrderRef", orderItem.getSubOrderNumber());
                            delivery.put("QrCodeNumber", orderItem.getQrCodeNumber());
                            orderRepository.findById(orderItem.getOrderId())
                                    .ifPresent(order -> {

                                    });
                        });
            }, () -> {

            });
            responseMap.put("statusCode", ResponseCodes.SUCCESS)
                    .put("statusDescription", "success")
                    .put("deliveryData", delivery)
                    .put("statusMessage", "Request Successful");
        } else {
            responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                    .put("statusDescription", "Merchant Does not exists")
                    .put("statusMessage", "Merchant Does not exists");
        }
        return ResponseEntity.ok(responseMap.toString());
    }


    // Confirm Customer Delivery
    @RequestMapping(value = "/generate-otp-code", method = RequestMethod.POST)
    public ResponseEntity<?> generateOtpCode(@RequestHeader("Authorization") String accessToken,
                                             @RequestBody String req) {
        JSONObject responseMap = new JSONObject();
        try {
            JSONObject valTokenReq = new JSONObject();
            valTokenReq.put("AccessToken", accessToken)
                    .put("UserType", "M");
            JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);
            if (valTokenBdy.getString("Status").equals("00")) {
                JSONObject userDetails = valTokenBdy.getJSONObject("UserDetails");
                JSONObject messengerInfo = userDetails.getJSONObject("messengerInfo");
                Long messengerId = Long.valueOf(messengerInfo.getInt("userId"));
                JSONObject request = new JSONObject(req);

                String[] deliveryTypes = {"MC", "MW"};
                itemMessengerDeliveryRepository.findItemMessengerDeliveryByOrderItemIdAndMessengerIdAndDeliveryTypeIn(request.getLong("OrderItemId"), messengerId, deliveryTypes
                ).ifPresentOrElse(itemMessengerDelivery -> {
                    orderItemRepository.findById(request.getLong("OrderItemId"))
                            .ifPresentOrElse(orderItem -> {
                                String validationCode = globalMethods.generateValidationCode();
                                itemMessengerDelivery.setValidationCode(globalMethods.encryptCode(validationCode));
                                itemMessengerDelivery.setValidationStatus(0);

                                orderItemRepository.save(orderItem);
                                responseMap.put("statusCode", ResponseCodes.SUCCESS)
                                        .put("statusDescription", "Success")
                                        .put("ValidCode", validationCode)
                                        .put("statusMessage", "Success");
                            }, () -> {
                                responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                                        .put("statusDescription", "The Item does not exists")
                                        .put("statusMessage", "The Item does not exists");
                            });
                }, () -> {
                    responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                            .put("statusDescription", "The Delivery is not assigned to this messenger")
                            .put("statusMessage", "The Delivery is not assigned to this messenger");
                });
            } else {
                responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                        .put("statusDescription", "Error in user validation")
                        .put("statusMessage", "Error in user validation");
            }
        } catch (Exception ex) {
            responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                    .put("statusDescription", "Error in user validation")
                    .put("statusMessage", "Error in user validation");
            log.log(Level.WARNING, ex.getMessage());
        }
        return ResponseEntity.ok(responseMap.toString());
    }

    // Confirm Customer Delivery
    @RequestMapping(value = "/attach-qr-code-item", method = RequestMethod.POST)
    public ResponseEntity<?> attachQrCodeToItem(@RequestHeader("Authorization") String accessToken,
                                                @RequestBody String req) {
        JSONObject responseMap = new JSONObject();
        try {
            JSONObject valTokenReq = new JSONObject();
            valTokenReq.put("AccessToken", accessToken)
                    .put("UserType", "M");
            JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);
            if (valTokenBdy.getString("Status").equals("00")) {
                JSONObject userDetails = valTokenBdy.getJSONObject("UserDetails");
                JSONObject messengerInfo = userDetails.getJSONObject("messengerInfo");
                Long messengerId = Long.valueOf(messengerInfo.getInt("userId"));
                JSONObject request = new JSONObject(req);

                String[] deliveryTypes = {"MC"};
                itemMessengerDeliveryRepository.findItemMessengerDeliveryByOrderItemIdAndMessengerIdAndDeliveryTypeIn(request.getLong("OrderItemId"), messengerId, deliveryTypes
                ).ifPresentOrElse(itemMessengerDelivery -> {
                    orderItemRepository.findById(request.getLong("OrderItemId"))
                            .ifPresentOrElse(orderItem -> {
                                orderItem.setIsQrCodeAssigned(1);
                                orderItem.setQrCodeNumber(request.getString("QrCodeNumber"));
                                orderItem.setQrCodeAssignmentDate(Timestamp.from(Instant.now()));
                                orderItemRepository.save(orderItem);
                                responseMap.put("statusCode", ResponseCodes.SUCCESS)
                                        .put("statusDescription", "Success")
                                        .put("statusMessage", "Success");
                            }, () -> {
                                responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                                        .put("statusDescription", "The Item does not exists")
                                        .put("statusMessage", "The Item does not exists");
                            });
                }, () -> {
                    responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                            .put("statusDescription", "The Delivery is not assigned to this messenger")
                            .put("statusMessage", "The Delivery is not assigned to this messenger");
                });
            } else {
                responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                        .put("statusDescription", "Error in user validation")
                        .put("statusMessage", "Error in user validation");
            }
        } catch (Exception ex) {
            responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                    .put("statusDescription", "Error in user validation")
                    .put("statusMessage", "Error in user validation");
            log.log(Level.WARNING, ex.getMessage());
        }
        return ResponseEntity.ok(responseMap.toString());
    }

    // Confirm Customer Delivery
    @RequestMapping(value = "/confirm-customer-delivery", method = RequestMethod.POST)
    public ResponseEntity<?> confirmCustomerDelivery(@RequestHeader("Authorization") String accessToken,
                                                     @RequestBody String req) {
        JSONObject responseMap = new JSONObject();
        try {
            JSONObject valTokenReq = new JSONObject();
            valTokenReq.put("AccessToken", accessToken)
                    .put("UserType", "M");
            JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);
            if (valTokenBdy.getString("Status").equals("00")) {
                JSONObject userDetails = valTokenBdy.getJSONObject("UserDetails");
                JSONObject messengerInfo = userDetails.getJSONObject("messengerInfo");
                Long messengerId = Long.valueOf(messengerInfo.getInt("userId"));
                JSONObject request = new JSONObject(req);

                String[] deliveryTypes = {"MC"};
                itemMessengerDeliveryRepository.findItemMessengerDeliveryByOrderItemIdAndMessengerIdAndDeliveryTypeIn(
                                request.getLong("OrderItemId"), messengerId, deliveryTypes)
                        .ifPresentOrElse(itemMessengerDelivery -> {
                            orderItemRepository.findById(request.getLong("OrderItemId"))
                                    .ifPresentOrElse(orderItem -> {
                                        itemMessengerDelivery.setStatus(3);
                                        itemMessengerDelivery.setUpdatedDate(Timestamp.from(Instant.now()));
                                        itemMessengerDeliveryRepository.save(itemMessengerDelivery);
                                        orderItem.setShipmentStatus(MessengerDeliveredItemToCustomer);
                                        orderItemRepository.save(orderItem);
                                        ItemShipmentStatus itemShipmentStatus = new ItemShipmentStatus();

                                        itemShipmentStatus.setShipmentStatus(MessengerDeliveredItemToCustomer);
                                        orderItem.setShipmentUpdateDate(Timestamp.from(Instant.now()));
                                        itemShipmentStatus.setItemId(orderItem.getItemId());
                                        itemShipmentStatus.setComments(request.getString("Comments"));
                                        itemShipmentStatus.setStatus(1);
                                        itemShipmentStatus.setCreatedDate(Timestamp.from(Instant.now()));
                                        itemShipmentStatusRepository.save(itemShipmentStatus);
                                        // Send Notification of Acceptance
                                        // merchantAcceptAndPickUpNotification.pickAndProcess(orderItem.getOrderId().toString());

                                        responseMap.put("statusCode", ResponseCodes.SUCCESS)
                                                .put("statusDescription", "Success")
                                                .put("statusMessage", "Success");
                                    }, () -> {
                                        responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                                                .put("statusDescription", "The Item does not exists")
                                                .put("statusMessage", "The Item does not exists");
                                    });
                        }, () -> {
                            responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                                    .put("statusDescription", "The Delivery is not assigned to this messenger")
                                    .put("statusMessage", "The Delivery is not assigned to this messenger");
                        });
            } else {
                responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                        .put("statusDescription", "Error in user validation")
                        .put("statusMessage", "Error in user validation");
            }
        } catch (Exception ex) {
            responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                    .put("statusDescription", "Error in user validation")
                    .put("statusMessage", "Error in user validation");
            log.log(Level.WARNING, ex.getMessage());
        }
        return ResponseEntity.ok(responseMap.toString());
    }
}
