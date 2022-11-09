package com.commerce.pal.payment.controller.user.messenger;

import com.commerce.pal.payment.model.shipping.ItemShipmentStatus;
import com.commerce.pal.payment.module.DataAccessService;
import com.commerce.pal.payment.module.ValidateAccessToken;
import com.commerce.pal.payment.module.order.OrderService;
import com.commerce.pal.payment.module.shipping.notification.process.MerchantAcceptAndPickUpNotification;
import com.commerce.pal.payment.repo.LoginValidationRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import static com.commerce.pal.payment.util.StatusCodes.*;

@Log
@CrossOrigin(origins = {"*"}, maxAge = 3600L)
@RestController
@RequestMapping({"/prime/api/v1/messenger/shipping"})
@SuppressWarnings("Duplicates")
public class MessengerShippingController {
    private final OrderService orderService;
    private final GlobalMethods globalMethods;
    private final OrderRepository orderRepository;
    private final DataAccessService dataAccessService;
    private final OrderItemRepository orderItemRepository;
    private final ValidateAccessToken validateAccessToken;
    private final ShipmentStatusRepository shipmentStatusRepository;
    private final LoginValidationRepository loginValidationRepository;
    private final ItemShipmentStatusRepository itemShipmentStatusRepository;
    private final ItemMessengerDeliveryRepository itemMessengerDeliveryRepository;
    private final MerchantAcceptAndPickUpNotification merchantAcceptAndPickUpNotification;

    @Autowired
    public MessengerShippingController(OrderService orderService,
                                       GlobalMethods globalMethods,
                                       OrderRepository orderRepository,
                                       DataAccessService dataAccessService,
                                       OrderItemRepository orderItemRepository,
                                       ValidateAccessToken validateAccessToken,
                                       ShipmentStatusRepository shipmentStatusRepository,
                                       LoginValidationRepository loginValidationRepository,
                                       ItemShipmentStatusRepository itemShipmentStatusRepository,
                                       ItemMessengerDeliveryRepository itemMessengerDeliveryRepository,
                                       MerchantAcceptAndPickUpNotification merchantAcceptAndPickUpNotification) {
        this.orderService = orderService;
        this.globalMethods = globalMethods;
        this.orderRepository = orderRepository;
        this.dataAccessService = dataAccessService;
        this.orderItemRepository = orderItemRepository;
        this.validateAccessToken = validateAccessToken;
        this.shipmentStatusRepository = shipmentStatusRepository;
        this.loginValidationRepository = loginValidationRepository;
        this.itemShipmentStatusRepository = itemShipmentStatusRepository;
        this.itemMessengerDeliveryRepository = itemMessengerDeliveryRepository;
        this.merchantAcceptAndPickUpNotification = merchantAcceptAndPickUpNotification;
    }


    @RequestMapping(value = {"/deliveries"}, method = {RequestMethod.GET}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> deliveries(@RequestHeader("Authorization") String accessToken,
                                        @RequestParam("status") Optional<Integer> status,
                                        @RequestParam("deliveryStatus") Optional<Integer> deliveryStatus) {
        JSONObject responseMap = new JSONObject();
        JSONObject valTokenReq = new JSONObject();
        valTokenReq.put("AccessToken", accessToken)
                .put("UserType", "M");
        JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);
        if (valTokenBdy.getString("Status").equals("00")) {
            JSONObject userDetails = valTokenBdy.getJSONObject("UserDetails");
            JSONObject messengerInfo = userDetails.getJSONObject("messengerInfo");
            Long messengerId = Long.valueOf(messengerInfo.getInt("userId"));


            List<Integer> acceptStatus = new ArrayList<>();
            status.ifPresentOrElse(accStatus -> {
                acceptStatus.add(accStatus);
            }, () -> {
                acceptStatus.add(0);
                acceptStatus.add(1);
            });

            List<Integer> delStatus = new ArrayList<>();
            deliveryStatus.ifPresentOrElse(accStatus -> {
                delStatus.add(accStatus);
            }, () -> {
                delStatus.add(0);
                delStatus.add(1);
                delStatus.add(3);
            });

            List<JSONObject> deliveryList = new ArrayList<>();

            itemMessengerDeliveryRepository.findItemMessengerDeliveriesByMessengerIdAndStatusInAndDeliveryStatusIn(
                    messengerId, globalMethods.convertListToIntegerArray(acceptStatus), globalMethods.convertListToIntegerArray(delStatus)
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
                delivery.put("DeliveryStatus", itemMessengerDelivery.getDeliveryStatus());
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

    @RequestMapping(value = "/accept-order-delivery", method = RequestMethod.POST)
    public ResponseEntity<?> acceptOrderDelivery(@RequestHeader("Authorization") String accessToken,
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
                itemMessengerDeliveryRepository.findItemMessengerDeliveryByIdAndMessengerId(
                        request.getLong("DeliveryId"), messengerId
                ).ifPresentOrElse(itemMessengerDelivery -> {
                    itemMessengerDelivery.setStatus(request.getInt("Status"));
                    itemMessengerDelivery.setAcceptedDate(Timestamp.from(Instant.now()));
                    itemMessengerDelivery.setAcceptanceRemarks(request.getString("Remarks"));
                    itemMessengerDeliveryRepository.save(itemMessengerDelivery);
                    responseMap.put("statusCode", ResponseCodes.SUCCESS)
                            .put("statusDescription", "Success")
                            .put("statusMessage", "Success");
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
                    .put("statusDescription", ex.getMessage())
                    .put("statusMessage", ex.getMessage());
            log.log(Level.WARNING, ex.getMessage());
        }
        return ResponseEntity.ok(responseMap.toString());
    }

    @RequestMapping(value = {"/order-item"}, method = {RequestMethod.GET}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> orderItem(@RequestParam("ItemId") String ItemId) {
        JSONObject orderItem = orderService.orderItemDetails(Long.valueOf(ItemId));
        return ResponseEntity.status(HttpStatus.OK).body(orderItem.toString());
    }

    @RequestMapping(value = {"/customer-address"}, method = {RequestMethod.GET}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> customerAddress(@RequestParam("ItemId") String ItemId) {
        JSONObject data = orderService.customerAddress(Long.valueOf(ItemId));
        return ResponseEntity.status(HttpStatus.OK).body(data.toString());
    }

    @RequestMapping(value = {"/merchant-address"}, method = {RequestMethod.GET}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> merchantAddress(@RequestParam("ItemId") String ItemId) {
        JSONObject data = orderService.merchantAddress(Long.valueOf(ItemId));
        return ResponseEntity.status(HttpStatus.OK).body(data.toString());
    }

    @RequestMapping(value = {"/product-info"}, method = {RequestMethod.GET}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> getProductInfo(@RequestParam("ItemId") String ItemId) {
        JSONObject orderItem = orderService.productInfo(Long.valueOf(ItemId));
        return ResponseEntity.status(HttpStatus.OK).body(orderItem.toString());
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
                                itemMessengerDelivery.setValidationStatus(1);
                                orderItemRepository.save(orderItem);
                                loginValidationRepository.findLoginValidationByEmailAddress(messengerInfo.getString("email"))
                                        .ifPresent(user -> {
                                            JSONObject pushPayload = new JSONObject();
                                            pushPayload.put("UserId", user.getUserOneSignalId() != null ? user.getUserOneSignalId() : "5c66ca50-c009-480f-a200-72c244d74ff4");
                                            pushPayload.put("Header", "Generate Code for : " + orderItem.getSubOrderNumber());
                                            pushPayload.put("Message", "Generate Code for : " + orderItem.getSubOrderNumber());
                                            JSONObject data = new JSONObject();
                                            data.put("OrderItem", orderItem.getSubOrderNumber());
                                            pushPayload.put("data", data);
                                            globalMethods.sendPushNotification(pushPayload);
                                        });

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
                    itemMessengerDelivery.setDeliveryStatus(1);
                    orderItemRepository.findById(request.getLong("OrderItemId"))
                            .ifPresentOrElse(orderItem -> {

                                orderItem.setIsQrCodeAssigned(1);
                                orderItem.setQrCodeNumber(request.getString("QrCodeNumber"));
                                orderItem.setQrCodeAssignmentDate(Timestamp.from(Instant.now()));
                                orderItemRepository.save(orderItem);
                                responseMap.put("statusCode", ResponseCodes.SUCCESS)
                                        .put("statusDescription", "Success")
                                        .put("statusMessage", "Success");

                                loginValidationRepository.findLoginValidationByEmailAddress(messengerInfo.getString("email"))
                                        .ifPresent(user -> {
                                            JSONObject pushPayload = new JSONObject();
                                            pushPayload.put("UserId", user.getUserOneSignalId() != null ? user.getUserOneSignalId() : "5c66ca50-c009-480f-a200-72c244d74ff4");
                                            pushPayload.put("Header", "Attach QR Code for : " + orderItem.getSubOrderNumber());
                                            pushPayload.put("Message", "Attach QR Code for : " + orderItem.getSubOrderNumber());
                                            JSONObject data = new JSONObject();
                                            data.put("OrderItem", orderItem.getSubOrderNumber());
                                            pushPayload.put("data", data);
                                            globalMethods.sendPushNotification(pushPayload);
                                        });
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

    @RequestMapping(value = "/generate-otp-code-customer-delivery", method = RequestMethod.POST)
    public ResponseEntity<?> generateOtpForCustomerDelivery(@RequestHeader("Authorization") String accessToken,
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
                itemMessengerDeliveryRepository.findItemMessengerDeliveryByOrderItemIdAndMessengerIdAndDeliveryTypeIn(
                        request.getLong("OrderItemId"), messengerId, deliveryTypes
                ).ifPresentOrElse(itemMessengerDelivery -> {
                    if (itemMessengerDelivery.getValidationStatus().equals(3)) {
                        orderItemRepository.findById(request.getLong("OrderItemId"))
                                .ifPresentOrElse(orderItem -> {
                                    String validationCode = globalMethods.generateValidationCode();
                                    itemMessengerDelivery.setDeliveryCode(globalMethods.encryptCode(validationCode));
                                    itemMessengerDelivery.setDeliveryStatus(3);
                                    orderItemRepository.save(orderItem);
                                    JSONObject emailPayload = new JSONObject();
                                    if (orderRepository.findByOrderId(orderItem.getOrderId()).getSaleType().equals("M2C")) {
                                        JSONObject cusReq = new JSONObject();
                                        cusReq.put("Type", "CUSTOMER");
                                        cusReq.put("TypeId", orderRepository.findByOrderId(orderItem.getOrderId()).getCustomerId());
                                        JSONObject cusRes = dataAccessService.pickAndProcess(cusReq);
                                        emailPayload.put("EmailDestination", cusRes.getString("email"));
                                        emailPayload.put("EmailMessage", "Delivery Validation Code : " + validationCode);
                                        globalMethods.processEmailWithoutTemplate(emailPayload);
                                    } else {
                                        JSONObject cusReq = new JSONObject();
                                        cusReq.put("Type", "BUSINESS");
                                        cusReq.put("TypeId", orderRepository.findByOrderId(orderItem.getOrderId()).getBusinessId());
                                        JSONObject cusRes = dataAccessService.pickAndProcess(cusReq);
                                        emailPayload.put("EmailDestination", cusRes.getString("email"));
                                        emailPayload.put("EmailMessage", "Delivery Validation Code : " + validationCode);
                                        globalMethods.processEmailWithoutTemplate(emailPayload);
                                    }

                                    responseMap.put("statusCode", ResponseCodes.SUCCESS)
                                            .put("statusDescription", "Success")
                                            .put("ValidCode", validationCode)
                                            .put("statusMessage", "Success");
                                }, () -> {
                                    responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                                            .put("statusDescription", "The Item does not exists")
                                            .put("statusMessage", "The Item does not exists");
                                });
                    } else {
                        responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                                .put("statusDescription", "The Item has not been picked")
                                .put("statusMessage", "The Item has not been picked");
                    }
                }, () -> {
                    responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                            .put("statusDescription", "The Item has not been picked")
                            .put("statusMessage", "The Item has not been picked");
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
                                        itemMessengerDelivery.setDeliveryStatus(3);
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

                                        loginValidationRepository.findLoginValidationByEmailAddress(messengerInfo.getString("email"))
                                                .ifPresent(user -> {
                                                    JSONObject pushPayload = new JSONObject();
                                                    pushPayload.put("UserId", user.getUserOneSignalId() != null ? user.getUserOneSignalId() : "5c66ca50-c009-480f-a200-72c244d74ff4");
                                                    pushPayload.put("Header", "Delivered to Customer : " + orderItem.getSubOrderNumber());
                                                    pushPayload.put("Message", "Delivered to Customer : " + orderItem.getSubOrderNumber());
                                                    JSONObject data = new JSONObject();
                                                    data.put("OrderItem", orderItem.getSubOrderNumber());
                                                    pushPayload.put("data", data);
                                                    globalMethods.sendPushNotification(pushPayload);
                                                });
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

    @RequestMapping(value = "/validate-business-code", method = RequestMethod.POST)
    public ResponseEntity<?> validatePickUpCode(@RequestHeader("Authorization") String accessToken,
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
                Long messenger = Long.valueOf(messengerInfo.getInt("userId"));
                JSONObject request = new JSONObject(req);

                String[] deliveryTypes = {"MC", "MW"};
                itemMessengerDeliveryRepository.findItemMessengerDeliveryByOrderItemIdAndMerchantIdAndDeliveryTypeIn(request.getLong("OrderItemId"), messenger, deliveryTypes
                ).ifPresentOrElse(itemMessengerDelivery -> {
                    orderItemRepository.findById(request.getLong("OrderItemId"))
                            .ifPresentOrElse(orderItem -> {
                                if (request.getString("ValidCode").equals(globalMethods.deCryptCode(itemMessengerDelivery.getValidationCode()))) {
                                    itemMessengerDelivery.setDeliveryStatus(1);
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
}
