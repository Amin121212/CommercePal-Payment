package com.commerce.pal.payment.controller.user.customer;

import com.commerce.pal.payment.module.DataAccessService;
import com.commerce.pal.payment.module.ValidateAccessToken;
import com.commerce.pal.payment.module.order.OrderService;
import com.commerce.pal.payment.repo.payment.OrderItemRepository;
import com.commerce.pal.payment.repo.payment.OrderRepository;
import com.commerce.pal.payment.repo.shipping.ItemShipmentStatusRepository;
import com.commerce.pal.payment.repo.shipping.ShipmentStatusRepository;
import com.commerce.pal.payment.util.ResponseCodes;
import com.commerce.pal.payment.util.specification.SpecificationsDao;
import com.commerce.pal.payment.util.specification.utils.SearchCriteria;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.commerce.pal.payment.util.TransactionStatus.PAYMENT_SUCCESS;

@Log
@CrossOrigin(origins = {"*"}, maxAge = 3600L)
@RestController
@RequestMapping({"/prime/api/v1/customer/order"})
@SuppressWarnings("Duplicates")
public class CustomerOrderController {
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final SpecificationsDao specificationsDao;
    private final DataAccessService dataAccessService;
    private final OrderItemRepository orderItemRepository;
    private final ValidateAccessToken validateAccessToken;
    private final ShipmentStatusRepository shipmentStatusRepository;
    private final ItemShipmentStatusRepository itemShipmentStatusRepository;

    @Autowired
    public CustomerOrderController(
            OrderService orderService,
            OrderRepository orderRepository,
            SpecificationsDao specificationsDao,
            DataAccessService dataAccessService,
            OrderItemRepository orderItemRepository,
            ValidateAccessToken validateAccessToken,
            ShipmentStatusRepository shipmentStatusRepository,
            ItemShipmentStatusRepository itemShipmentStatusRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.specificationsDao = specificationsDao;
        this.dataAccessService = dataAccessService;
        this.orderItemRepository = orderItemRepository;
        this.validateAccessToken = validateAccessToken;
        this.shipmentStatusRepository = shipmentStatusRepository;
        this.itemShipmentStatusRepository = itemShipmentStatusRepository;
    }

    @RequestMapping(value = {"/my-orders"}, method = {RequestMethod.POST}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> customerOrders(@RequestHeader("Authorization") String accessToken,
                                            @RequestParam("page") Optional<String> orderRef) {
        JSONObject responseMap = new JSONObject();

        List<JSONObject> orders = new ArrayList<>();
        JSONObject valTokenReq = new JSONObject();
        valTokenReq.put("AccessToken", accessToken)
                .put("UserType", "B");

        JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);

        if (valTokenBdy.getString("Status").equals("00")) {
            JSONObject customerData = valTokenBdy.getJSONObject("UserDetails").getJSONObject("Details");
            Long customerId = customerData.getLong("userId");
            List<SearchCriteria> params = new ArrayList<SearchCriteria>();
            params.add(new SearchCriteria("customerId", ":", customerId));
            params.add(new SearchCriteria("status", ":", PAYMENT_SUCCESS));
            params.add(new SearchCriteria("paymentStatus", ":", PAYMENT_SUCCESS));
            orderRef.ifPresent(value -> {
                params.add(new SearchCriteria("orderRef", ":", value));
            });
            specificationsDao.getOrders(params)
                    .forEach(order -> {
                        JSONObject orderInfo = new JSONObject();
                        orderInfo.put("OrderId", order.getOrderId());
                        orderInfo.put("OrderRef", order.getOrderRef());
                        orderInfo.put("OrderDate", order.getOrderDate());
                        orderInfo.put("DeliveryPrice", order.getDeliveryPrice());
                        orderInfo.put("TotalPrice", order.getTotalPrice());
                        orderInfo.put("PaymentStatus", order.getPaymentStatus());
                        orderInfo.put("PaymentDate", order.getPaymentDate());
                        orderInfo.put("PaymentMethod", order.getPaymentMethod());
                        orderInfo.put("Discount", order.getDiscount());
                        orderInfo.put("DeliveryPrice", order.getDeliveryPrice());
                        orders.add(orderInfo);
                    });
        }
        responseMap.put("statusCode", ResponseCodes.SUCCESS)
                .put("statusDescription", "success")
                .put("data", orders)
                .put("statusMessage", "Request Successful");
        return ResponseEntity.ok(responseMap.toString());
    }

    @RequestMapping(value = {"/order-detail"}, method = {RequestMethod.POST}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> orderDetails(@RequestHeader("Authorization") String accessToken, @RequestBody String req) {
        JSONObject responseMap = new JSONObject();
        JSONObject reqBdy = new JSONObject(req);

        JSONObject orderDetails = new JSONObject();
        JSONObject valTokenReq = new JSONObject();
        valTokenReq.put("AccessToken", accessToken)
                .put("UserType", "C");
        JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);

        if (valTokenBdy.getString("Status").equals("00")) {
            JSONObject customerData = valTokenBdy.getJSONObject("UserDetails").getJSONObject("Details");
            Long customerId = customerData.getLong("userId");
            orderRepository.findOrderByOrderIdAndCustomerId(reqBdy.getLong("OrderId"), customerId)
                    .ifPresent(order -> {

                        orderDetails.put("OrderRef", order.getOrderRef());
                        orderDetails.put("OrderDate", order.getOrderDate());
                        orderDetails.put("DeliveryPrice", order.getDeliveryPrice());
                        orderDetails.put("TotalPrice", order.getTotalPrice());
                        orderDetails.put("PaymentStatus", order.getPaymentStatus());
                        orderDetails.put("PaymentDate", order.getPaymentDate());
                        orderDetails.put("PaymentMethod", order.getPaymentMethod());
                        orderDetails.put("Discount", order.getDiscount());
                        orderDetails.put("DeliveryPrice", order.getDeliveryPrice());
                        List<JSONObject> orderItems = new ArrayList<>();
                        orderItemRepository.findOrderItemsByOrderId(order.getOrderId())
                                .forEach(orderItem -> {
                                    JSONObject prodReq = new JSONObject();
                                    prodReq.put("Type", "PRODUCT-AND-SUB");
                                    prodReq.put("TypeId", orderItem.getProductLinkingId());
                                    prodReq.put("SubProductId", orderItem.getSubProductId());
                                    JSONObject prodRes = dataAccessService.pickAndProcess(prodReq);
                                    JSONObject itemPay = orderService.orderItemDetails(orderItem.getItemId());
                                    itemPay.put("Product", prodRes);
                                    orderItems.add(itemPay);
                                });
                        orderDetails.put("orderItems", orderItems);
                    });
        }
        responseMap.put("statusCode", ResponseCodes.SUCCESS)
                .put("statusDescription", "success")
                .put("data", orderDetails)
                .put("statusMessage", "Request Successful");
        return ResponseEntity.ok(responseMap.toString());
    }

    @RequestMapping(value = {"/order-summary"}, method = {RequestMethod.GET}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> orderSummary(@RequestHeader("Authorization") String accessToken,
                                          @RequestParam("status") Integer status) {
        JSONObject responseMap = new JSONObject();
        JSONObject valTokenReq = new JSONObject();
        valTokenReq.put("AccessToken", accessToken)
                .put("UserType", "M");

        JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);

        if (valTokenBdy.getString("Status").equals("00")) {
            JSONObject userDetails = valTokenBdy.getJSONObject("UserDetails");
            JSONObject merchantInfo = userDetails.getJSONObject("merchantInfo");// userDetails.getJSONObject("messengerInfo");
            Long merchantId = Long.valueOf(userDetails.getJSONObject("merchantInfo").getInt("userId"));
            List<Integer> shipmentStatus = new ArrayList<>();
            if (status.equals(10)) {
                shipmentStatus.add(0);
                shipmentStatus.add(1);
                shipmentStatus.add(2);
                shipmentStatus.add(3);
                shipmentStatus.add(4);
                shipmentStatus.add(5);
            } else {
                shipmentStatus.add(status);
            }
            List<JSONObject> orders = new ArrayList<>();
            orderItemRepository.findByMerchantIdAndUserShipmentStatus(merchantId, shipmentStatus)
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

                                    List<JSONObject> orderItems = new ArrayList<>();
                                    orderItemRepository.findOrderItemsByOrderIdAndMerchantId(order.getOrderId(), merchantId)
                                            .forEach(orderItem -> {
                                                JSONObject itemPay = new JSONObject();
                                                JSONObject prodReq = new JSONObject();
                                                prodReq.put("Type", "PRODUCT");
                                                prodReq.put("TypeId", orderItem.getProductLinkingId());
                                                JSONObject prodRes = dataAccessService.pickAndProcess(prodReq);
                                                itemPay.put("NoOfProduct", orderItem.getQuantity());
                                                itemPay.put("ItemOrderRef", orderItem.getSubOrderNumber());
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


    @RequestMapping(value = {"/order-item"}, method = {RequestMethod.GET}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> orderItem(@RequestParam("ItemId") String ItemId) {
        JSONObject orderItem = orderService.orderItemDetails(Long.valueOf(ItemId));
        return ResponseEntity.status(HttpStatus.OK).body(orderItem.toString());
    }

    @RequestMapping(value = {"/order-status-item"}, method = {RequestMethod.GET}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> getItemShippingStatus(@RequestHeader("Authorization") String accessToken,
                                                   @RequestParam("OrderItemId") Integer OrderItemId) {
        JSONObject responseMap = new JSONObject();
        JSONObject valTokenReq = new JSONObject();
        valTokenReq.put("AccessToken", accessToken)
                .put("UserType", "M");

        JSONObject valTokenBdy = validateAccessToken.pickAndReturnAll(valTokenReq);

        if (valTokenBdy.getString("Status").equals("00")) {
            JSONObject userDetails = valTokenBdy.getJSONObject("UserDetails");
            JSONObject orderItem = new JSONObject();
            orderItemRepository.findById(Long.valueOf(OrderItemId)).ifPresent(item -> {
                orderItem.put("ItemId", item.getItemId());
                orderItem.put("OrderId", item.getOrderId());
                orderItem.put("SubOrderNumber", item.getSubOrderNumber());
                orderItem.put("SubProductId", item.getSubProductId());
                orderItem.put("ProductId", item.getProductLinkingId());
                orderItem.put("UnitPrice", item.getUnitPrice());
                orderItem.put("TotalAmount", item.getTotalAmount());
                orderItem.put("QrCodeNumber", item.getQrCodeNumber());
                orderItem.put("CreatedDate", item.getCreatedDate());
                orderItem.put("ShipmentStatus", item.getShipmentStatus());
                shipmentStatusRepository.findShipmentStatusByCode(item.getShipmentStatus())
                        .ifPresentOrElse(shipmentStatus -> {
                            orderItem.put("ShipmentStatusWord", shipmentStatus.getDescription());
                        }, () -> {
                            orderItem.put("ShipmentStatusWord", "Processing on WareHouse");
                        });

                List<String> shipmentStatus = new ArrayList<>();
                itemShipmentStatusRepository.findItemShipmentStatusesByItemId(item.getItemId())
                        .forEach(itemShipmentStatus -> {
                            shipmentStatusRepository.findShipmentStatusByCode(itemShipmentStatus.getShipmentStatus())
                                    .ifPresentOrElse(shipmentStatus1 -> {
                                        shipmentStatus.add(shipmentStatus1.getDescription());
                                    }, () -> {
                                        shipmentStatus.add("Processing on WareHouse");
                                    });
                        });
                orderItem.put("ShipmentStatusList", shipmentStatus);
            });
            responseMap.put("statusCode", ResponseCodes.SUCCESS)
                    .put("statusDescription", "success")
                    .put("data", orderItem)
                    .put("statusMessage", "Request Successful");
        } else {
            responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                    .put("statusDescription", "Merchant Does not exists")
                    .put("statusMessage", "Merchant Does not exists");
        }
        return ResponseEntity.ok(responseMap.toString());
    }
}
