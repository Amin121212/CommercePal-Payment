package com.commerce.pal.payment.controller.portal.admin;

import com.commerce.pal.payment.module.DataAccessService;
import com.commerce.pal.payment.repo.payment.OrderItemRepository;
import com.commerce.pal.payment.repo.payment.OrderRepository;
import com.commerce.pal.payment.repo.portal.WareHouseRepository;
import com.commerce.pal.payment.repo.shipping.ShipmentStatusRepository;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import static com.commerce.pal.payment.util.StatusCodes.INITIAL_PAGE;
import static com.commerce.pal.payment.util.StatusCodes.MIN_PAGE_SIZE;
import static com.commerce.pal.payment.util.TransactionStatus.PAYMENT_SUCCESS;

@Log
@CrossOrigin(origins = {"*"}, maxAge = 3600L)
@RestController
@RequestMapping({"/prime/api/v1/portal/shipping"})
@SuppressWarnings("Duplicates")
public class PortalShippingController {
    private final OrderRepository orderRepository;
    private final DataAccessService dataAccessService;
    private final OrderItemRepository orderItemRepository;
    private final WareHouseRepository wareHouseRepository;
    private final ShipmentStatusRepository shipmentStatusRepository;

    @Autowired
    public PortalShippingController(OrderRepository orderRepository,
                                    DataAccessService dataAccessService,
                                    OrderItemRepository orderItemRepository,
                                    WareHouseRepository wareHouseRepository,
                                    ShipmentStatusRepository shipmentStatusRepository) {
        this.orderRepository = orderRepository;
        this.dataAccessService = dataAccessService;
        this.orderItemRepository = orderItemRepository;
        this.wareHouseRepository = wareHouseRepository;
        this.shipmentStatusRepository = shipmentStatusRepository;
    }

    @RequestMapping(value = {"/get-order"}, method = {RequestMethod.GET}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> getOrders(@RequestParam("status") Integer status,
                                       @RequestParam("page") Optional<Integer> page) {
        JSONObject responseMap = new JSONObject();
        int evalPage = (page.orElse(0) < 1) ? INITIAL_PAGE : page.get() - 1;
        List<Integer> shipmentStatus = new ArrayList<>();
        if (status.equals(000)) {
            shipmentStatus = shipmentStatusRepository.findShipmentStatusByCodeAndCode("c", "c");
        } else {
            shipmentStatus.add(status);
        }
        List<JSONObject> orders = new ArrayList<>();
        orderRepository.findOrdersByStatusAndPaymentStatusOrderByOrderIdDesc(
                PAYMENT_SUCCESS, PAYMENT_SUCCESS, PageRequest.of(evalPage, MIN_PAGE_SIZE)
        ).forEach(order -> {
            try {
                JSONObject orderDetails = new JSONObject();
                if (order.getSaleType().equals("M2C")) {
                    JSONObject cusReq = new JSONObject();
                    cusReq.put("Type", "CUSTOMER");
                    cusReq.put("TypeId", order.getCustomerId());
                    JSONObject cusRes = dataAccessService.pickAndProcess(cusReq);
                    orderDetails.put("CustomerName", cusRes.getString("firstName"));
                } else {
                    JSONObject cusReq = new JSONObject();
                    cusReq.put("Type", "BUSINESS");
                    cusReq.put("TypeId", order.getBusinessId());
                    JSONObject cusRes = dataAccessService.pickAndProcess(cusReq);
                    orderDetails.put("CustomerName", cusRes.getString("firstName"));
                }

                orderDetails.put("OrderRef", order.getOrderRef());
                orderDetails.put("OrderDate", order.getOrderDate());
                orderDetails.put("Order", order.getOrderRef());
                orderDetails.put("OrderId", order.getOrderId());
                orderDetails.put("SaleType", order.getSaleType());
                orderDetails.put("PaymentMethod", order.getPaymentMethod());

                List<JSONObject> orderItems = new ArrayList<>();
                orderItemRepository.findOrderItemsByOrderId(order.getOrderId())
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
            } catch (Exception ex) {
                log.log(Level.WARNING, "OrderId [" + order.getOrderId() + "]" +  ex.getMessage());
            }
        });
        responseMap.put("statusCode", ResponseCodes.SUCCESS)
                .put("statusDescription", "Request Successful")
                .put("data", orders)
                .put("statusMessage", "Request Successful");
        return ResponseEntity.ok(responseMap.toString());
    }

    @RequestMapping(value = {"/get-order-info"}, method = {RequestMethod.GET}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> getOrderInfo(@RequestParam("OrderId") Long OrderId) {
        JSONObject responseMap = new JSONObject();
        JSONObject orderDetails = new JSONObject();
        orderRepository.findById(OrderId)
                .ifPresent(order -> {
                    JSONObject cusReq = new JSONObject();
                    cusReq.put("Type", "CUSTOMER");
                    cusReq.put("TypeId", order.getCustomerId());
                    JSONObject cusRes = dataAccessService.pickAndProcess(cusReq);
                    orderDetails.put("CustomerName", cusRes.getString("firstName"));
                    orderDetails.put("OrderRef", order.getOrderRef());
                    orderDetails.put("OrderDate", order.getOrderDate());
                    orderDetails.put("Order", order.getOrderRef());
                    orderDetails.put("CustomerData", cusRes);
                    List<JSONObject> orderItems = new ArrayList<>();
                    orderItemRepository.findOrderItemsByOrderId(order.getOrderId())
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
                });
        responseMap.put("statusCode", ResponseCodes.SUCCESS)
                .put("statusDescription", "success")
                .put("orderDetails", orderDetails)
                .put("statusMessage", "Request Successful");
        return ResponseEntity.ok(responseMap.toString());
    }

    //Admin Configure Shipping process and the respective WareHouse
    @RequestMapping(value = "/shipping-configuration", method = RequestMethod.POST)
    public ResponseEntity<?> shippingConfiguration(@RequestBody String req) {
        JSONObject responseMap = new JSONObject();
        try {
            JSONObject request = new JSONObject(req);
            orderItemRepository.findById(request.getLong("OrderItemId"))
                    .ifPresentOrElse(orderItem -> {
                        orderItem.setAssignedWareHouseId(request.getInt("AssignedWareHouseId"));
                        orderItem.setShipmentType(request.getString("ShipmentType"));
                        orderItem.setFinalizingWareHouseId(request.getInt("FinalizingWareHouseId"));
                        orderItem.setShipmentTypeComments(request.getString("ShipmentTypeComments"));
                        orderItemRepository.save(orderItem);
                        responseMap.put("statusCode", ResponseCodes.SUCCESS)
                                .put("statusDescription", "Configuration was successfully")
                                .put("statusMessage", "Configuration was successfully");
                    }, () -> {
                        responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                                .put("statusDescription", "Order Item Does not Exists")
                                .put("statusMessage", "Order Item Does not Exists");
                    });
        } catch (Exception ex) {
            responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                    .put("statusDescription", "Order Item Does not Exists")
                    .put("statusMessage", "Order Item Does not Exists");
            log.log(Level.WARNING, ex.getMessage());
        }
        return ResponseEntity.ok(responseMap.toString());
    }

    @RequestMapping(value = {"/banks"}, method = {RequestMethod.GET}, produces = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> bankList() {
        List<JSONObject> wareHouse = new ArrayList<>();
        wareHouseRepository.findAll().forEach(wareHouse1 -> {
            JSONObject one = new JSONObject();
            one.put("name", wareHouse1.getWareHouseName());
            one.put("id", wareHouse1.getId());
            wareHouse.add(one);
        });
        return ResponseEntity.status(HttpStatus.OK).body(wareHouse.toString());
    }




}
