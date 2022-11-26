package com.commerce.pal.payment.controller.user.merchant;

import com.commerce.pal.payment.model.shipping.ItemShipmentStatus;
import com.commerce.pal.payment.module.DataAccessService;
import com.commerce.pal.payment.module.ValidateAccessToken;
import com.commerce.pal.payment.module.database.PaymentStoreProcedure;
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
import static com.commerce.pal.payment.util.StatusCodes.MessengerPickedMerchantToWareHouse;

@Log
@CrossOrigin(origins = {"*"}, maxAge = 3600L)
@RestController
@RequestMapping({"/prime/api/v1/merchant/shipping"})
@SuppressWarnings("Duplicates")
public class MerchantShippingController {
    private final GlobalMethods globalMethods;
    private final OrderRepository orderRepository;
    private final DataAccessService dataAccessService;
    private final OrderItemRepository orderItemRepository;
    private final ValidateAccessToken validateAccessToken;
    private final PaymentStoreProcedure paymentStoreProcedure;
    private final ShipmentStatusRepository shipmentStatusRepository;
    private final ItemShipmentStatusRepository itemShipmentStatusRepository;
    private final ItemMessengerDeliveryRepository itemMessengerDeliveryRepository;
    private final MerchantAcceptAndPickUpNotification merchantAcceptAndPickUpNotification;

    @Autowired
    public MerchantShippingController(GlobalMethods globalMethods,
                                      OrderRepository orderRepository,
                                      DataAccessService dataAccessService,
                                      OrderItemRepository orderItemRepository,
                                      ValidateAccessToken validateAccessToken,
                                      PaymentStoreProcedure paymentStoreProcedure,
                                      ShipmentStatusRepository shipmentStatusRepository,
                                      ItemShipmentStatusRepository itemShipmentStatusRepository,
                                      ItemMessengerDeliveryRepository itemMessengerDeliveryRepository,
                                      MerchantAcceptAndPickUpNotification merchantAcceptAndPickUpNotification) {
        this.globalMethods = globalMethods;
        this.orderRepository = orderRepository;
        this.dataAccessService = dataAccessService;
        this.orderItemRepository = orderItemRepository;
        this.validateAccessToken = validateAccessToken;
        this.paymentStoreProcedure = paymentStoreProcedure;
        this.shipmentStatusRepository = shipmentStatusRepository;
        this.itemShipmentStatusRepository = itemShipmentStatusRepository;
        this.itemMessengerDeliveryRepository = itemMessengerDeliveryRepository;
        this.merchantAcceptAndPickUpNotification = merchantAcceptAndPickUpNotification;
    }

    // Filter New Request
    @RequestMapping(value = {"/filter-order"}, method = {RequestMethod.GET}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> filterOrder(@RequestHeader("Authorization") String accessToken,
                                         @RequestParam("status") Integer status) {
        JSONObject responseMap = new JSONObject();
        JSONObject valTokenReq = new JSONObject();
        valTokenReq.put("AccessToken", accessToken)
                .put("UserType", "M");

        JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);

        if (valTokenBdy.getString("Status").equals("00")) {
            JSONObject userDetails = valTokenBdy.getJSONObject("UserDetails");
            JSONObject merchantInfo = userDetails.getJSONObject("merchantInfo");
            Long merchantId = Long.valueOf(merchantInfo.getInt("userId"));

            List<Integer> shipmentStatus = new ArrayList<>();
            if (status.equals(000)) {
                shipmentStatus = shipmentStatusRepository.findShipmentStatusByCodeAndCode("c", "c");
            } else {
                shipmentStatus.add(status);
            }
            List<JSONObject> orders = new ArrayList<>();
            orderItemRepository.findByMerchantIdAndShipmentStatus(merchantId, shipmentStatus)
                    .forEach(orderItemParent -> {
                        orderRepository.findOrderByOrderId(orderItemParent)
                                .ifPresent(order -> {
                                    JSONObject orderDetails = new JSONObject();
                                    JSONObject cusReq = new JSONObject();
                                    cusReq.put("Type", "CUSTOMER");
                                    cusReq.put("TypeId", order.getCustomerId());
                                    JSONObject cusRes = dataAccessService.pickAndProcess(cusReq);
                                    orderDetails.put("CustomerName", cusRes.getString("firstName"));
                                    orderDetails.put("OrderRef", order.getOrderRef());
                                    orderDetails.put("OrderDate", order.getOrderDate());
                                    orderDetails.put("Order", order.getOrderRef());
                                    List<JSONObject> orderItems = new ArrayList<>();
                                    orderItemRepository.findOrderItemsByOrderIdAndMerchantId(order.getOrderId(), merchantId)
                                            .forEach(orderItem -> {
                                                JSONObject itemPay = new JSONObject();
                                                JSONObject prodReq = new JSONObject();
                                                prodReq.put("Type", "PRODUCT");
                                                prodReq.put("TypeId", orderItem.getProductLinkingId());
                                                JSONObject prodRes = dataAccessService.pickAndProcess(prodReq);

                                                JSONObject subProdReq = new JSONObject();
                                                subProdReq.put("Type", "SUB-PRODUCT");
                                                subProdReq.put("TypeId", orderItem.getSubProductId());
                                                JSONObject subProdRes = dataAccessService.pickAndProcess(subProdReq);

                                                itemPay.put("OrderItemId", orderItem.getItemId());
                                                itemPay.put("NoOfProduct", orderItem.getQuantity());
                                                itemPay.put("ItemOrderRef", orderItem.getSubOrderNumber());
                                                itemPay.put("ShippingStatus", orderItem.getShipmentStatus());

                                                itemPay.put("productDetails", prodRes);
                                                itemPay.put("subProductDetails", subProdRes);

                                                orderItems.add(itemPay);

                                            });
                                    orderDetails.put("orderItems", orderItems);
                                    orders.add(orderDetails);
                                });
                    });
            responseMap.put("statusCode", ResponseCodes.SUCCESS)
                    .put("statusDescription", "success")
                    .put("data", orders)
                    .put("statusMessage", "Request Successful");
        } else {
            responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                    .put("statusDescription", "Merchant Does not exists")
                    .put("statusMessage", "Merchant Does not exists");
        }
        return ResponseEntity.ok(responseMap.toString());
    }

    // Accept Order Item and Item PickUp  Request
    @RequestMapping(value = "/accept-item-pickup", method = RequestMethod.POST)
    public ResponseEntity<?> acceptReadyPickUp(@RequestHeader("Authorization") String accessToken,
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
                orderItemRepository.findOrderItemByItemIdAndMerchantId(
                                request.getLong("ItemId"), merchantId)
                        .ifPresentOrElse(orderItem -> {
                            orderItem.setShipmentStatus(AcceptReadyForPickUp);
                            orderItem.setMerchantPickingDate(request.getString("MerchantPickingDate"));
                            orderItem.setShipmentUpdateDate(Timestamp.from(Instant.now()));

                            ItemShipmentStatus itemShipmentStatus = new ItemShipmentStatus();
                            itemShipmentStatus.setItemId(orderItem.getItemId());
                            itemShipmentStatus.setShipmentStatus(AcceptReadyForPickUp);
                            itemShipmentStatus.setComments(request.getString("Comments"));
                            itemShipmentStatus.setStatus(1);
                            itemShipmentStatus.setCreatedDate(Timestamp.from(Instant.now()));
                            itemShipmentStatusRepository.save(itemShipmentStatus);
                            // Send Notification of Acceptance
                            merchantAcceptAndPickUpNotification.pickAndProcess(orderItem.getItemId());

                            responseMap.put("statusCode", ResponseCodes.SUCCESS)
                                    .put("statusDescription", "success")
                                    .put("statusMessage", "success");
                        }, () -> {
                            responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                                    .put("statusDescription", "Merchant Does not exists")
                                    .put("statusMessage", "Merchant Does not exists");
                        });
            } else {
                responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                        .put("statusDescription", "Merchant Does not exists")
                        .put("statusMessage", "Merchant Does not exists");
            }
        } catch (Exception ex) {
            responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                    .put("statusDescription", "Merchant Does not exists")
                    .put("statusMessage", "Merchant Does not exists");
            log.log(Level.WARNING, ex.getMessage());
        }
        return ResponseEntity.ok(responseMap.toString());
    }

    // QR Code for Messenger PickUp
    @RequestMapping(value = "/validate-pick-up-code", method = RequestMethod.POST)
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
                JSONObject merchantInfo = userDetails.getJSONObject("merchantInfo");
                Long merchant = Long.valueOf(merchantInfo.getInt("userId"));
                JSONObject request = new JSONObject(req);

                String[] deliveryTypes = {"MC", "MW"};
                itemMessengerDeliveryRepository.findItemMessengerDeliveryByOrderItemIdAndMerchantIdAndDeliveryTypeIn(request.getLong("OrderItemId"), merchant, deliveryTypes
                ).ifPresentOrElse(itemMessengerDelivery -> {
                    orderItemRepository.findById(request.getLong("OrderItemId"))
                            .ifPresentOrElse(orderItem -> {
                                if (request.getString("ValidCode").equals(globalMethods.deCryptCode(itemMessengerDelivery.getValidationCode()))) {
                                    itemMessengerDelivery.setValidationStatus(3);
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

                                    // Process Payment
                                    String transRef = globalMethods.generateTrans();
                                    String payNar = "Settlement of Sub Order [" + orderItem.getSubOrderNumber() + "]";
                                    JSONObject reqBody = new JSONObject();
                                    reqBody.put("TransRef", transRef);
                                    reqBody.put("ItemId", orderItem.getItemId().toString());
                                    reqBody.put("PaymentNarration", payNar);

                                    JSONObject payRes = paymentStoreProcedure.merchantItemSettlement(reqBody);
                                    responseMap.put("statusCode", ResponseCodes.SUCCESS)
                                            .put("balance", payRes.getString("Balance"))
                                            .put("comBalance", payRes.getString("ComBalance"))
                                            .put("transRef", transRef)
                                            .put("statusDescription", "Success")
                                            .put("statusMessage", "Success");

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
                            .put("statusDescription", "The Delivery is not assigned to this merchant/messenger")
                            .put("statusMessage", "The Delivery is not assigned to this merchant/messenger");
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
