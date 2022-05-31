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
import java.util.logging.Level;

import static com.commerce.pal.payment.util.StatusCodes.*;

@Log
@CrossOrigin(origins = {"*"}, maxAge = 3600L)
@RestController
@RequestMapping({"/prime/api/v1/merchant/shipping"})
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

    // QR Code for Messenger PickUp
    @RequestMapping(value = "/validate-merchant-code", method = RequestMethod.POST)
    public ResponseEntity<?> validateMerchantCode(@RequestHeader("Authorization") String accessToken,
                                                   @RequestBody String req) {
        JSONObject responseMap = new JSONObject();
        try {
            JSONObject valTokenReq = new JSONObject();
            valTokenReq.put("AccessToken", accessToken)
                    .put("UserType", "M");
            JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);
            if (valTokenBdy.getString("Status").equals("00")) {
                JSONObject userDetails = valTokenBdy.getJSONObject("UserDetails");
                JSONObject merchantInfo = userDetails.getJSONObject("merchantInfo");
                Long merchantId = Long.valueOf(merchantInfo.getInt("userId"));
                JSONObject request = new JSONObject(req);

                String[] deliveryTypes = {"MC", "MW"};
                itemMessengerDeliveryRepository.findItemMessengerDeliveryByOrderItemIdAndMerchantIdAndDeliveryTypeIn(request.getLong("ItemId"), merchantId, deliveryTypes
                ).ifPresentOrElse(itemMessengerDelivery -> {
                    orderItemRepository.findById(request.getLong("ItemId"))
                            .ifPresentOrElse(orderItem -> {
                                if (request.getString("ValidCode").equals(globalMethods.deCryptCode(itemMessengerDelivery.getValidationCode()))) {
                                    itemMessengerDelivery.setStatus(1);
                                    itemMessengerDelivery.setValidationStatus(1);
                                    itemMessengerDelivery.setValidationDate(Timestamp.from(Instant.now()));
                                    itemMessengerDeliveryRepository.save(itemMessengerDelivery);

                                    ItemShipmentStatus itemShipmentStatus = new ItemShipmentStatus();
                                    if (itemMessengerDelivery.getDeliveryType().equals("MC")) {
                                        orderItem.setShipmentStatus(MessengerPickedMerchantToCustomer);
                                        itemShipmentStatus.setShipmentStatus(MessengerPickedMerchantToCustomer);
                                    } else {
                                        orderItem.setShipmentStatus(MessengerPickedMerchantToWareHouse);
                                        itemShipmentStatus.setShipmentStatus(MessengerPickedMerchantToWareHouse);
                                    }
                                    orderItem.setShipmentUpdateDate(Timestamp.from(Instant.now()));
                                    orderItemRepository.save(orderItem);

                                    itemShipmentStatus.setItemId(orderItem.getItemId());
                                    itemShipmentStatus.setComments(request.getString("Comments"));
                                    itemShipmentStatus.setStatus(1);
                                    itemShipmentStatus.setCreatedDate(Timestamp.from(Instant.now()));
                                    itemShipmentStatusRepository.save(itemShipmentStatus);

                                    responseMap.put("statusCode", ResponseCodes.SUCCESS)
                                            .put("statusDescription", "success")
                                            .put("statusMessage", "success");

                                    // Send Notification of Acceptance
                                    // merchantAcceptAndPickUpNotification.pickAndProcess(orderItem.getOrderId().toString());
                                } else {
                                    responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                                            .put("statusDescription", "The code is not valid")
                                            .put("statusMessage", "The code is not valid");
                                }
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
                itemMessengerDeliveryRepository.findItemMessengerDeliveryByOrderItemIdAndMessengerIdAndDeliveryTypeIn(request.getLong("ItemId"), messengerId, deliveryTypes
                ).ifPresentOrElse(itemMessengerDelivery -> {
                    orderItemRepository.findById(request.getLong("ItemId"))
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
                itemMessengerDeliveryRepository.findItemMessengerDeliveryByOrderItemIdAndMessengerIdAndDeliveryTypeIn(request.getLong("ItemId"), messengerId, deliveryTypes
                ).ifPresentOrElse(itemMessengerDelivery -> {
                    orderItemRepository.findById(request.getLong("ItemId"))
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
