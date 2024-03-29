package com.commerce.pal.payment.module.order;

import com.commerce.pal.payment.module.DataAccessService;
import com.commerce.pal.payment.repo.payment.OrderItemRepository;
import com.commerce.pal.payment.repo.payment.OrderRepository;
import com.commerce.pal.payment.repo.shipping.ShipmentStatusRepository;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

@Log
@Service
@SuppressWarnings("Duplicates")
public class OrderService {

    private final OrderRepository orderRepository;
    private final DataAccessService dataAccessService;
    private final OrderItemRepository orderItemRepository;
    private final ShipmentStatusRepository shipmentStatusRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository,
                        DataAccessService dataAccessService,
                        OrderItemRepository orderItemRepository,
                        ShipmentStatusRepository shipmentStatusRepository) {
        this.orderRepository = orderRepository;
        this.dataAccessService = dataAccessService;
        this.orderItemRepository = orderItemRepository;
        this.shipmentStatusRepository = shipmentStatusRepository;
    }

    public JSONObject orderItemDetails(Long itemId) {
        JSONObject orderItem = new JSONObject();
        try {
            orderItemRepository.findById(itemId).ifPresent(item -> {
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
                orderItem.put("ShipmentStatusWord", shipmentStatusRepository.findById(item.getShipmentStatus()).get().getDescription());
            });
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
        }
        return orderItem;
    }

    public JSONObject customerAddressAdmin(Long orderId) {
        AtomicReference<JSONObject> customerAddress = new AtomicReference<>(new JSONObject());
        try {
            orderRepository.findById(orderId)
                    .ifPresent(order -> {
                        JSONObject cusReq = new JSONObject();
                        cusReq.put("Type", "CUSTOMER-ADDRESS");
                        cusReq.put("TypeId", order.getUserAddressId());
                        customerAddress.set(dataAccessService.pickAndProcess(cusReq));
                    });

        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
        }
        return customerAddress.get();
    }


    public JSONObject customerAddress(Long orderId) {
        AtomicReference<JSONObject> customerAddress = new AtomicReference<>(new JSONObject());
        try {
            orderItemRepository.findById(orderId).ifPresent(order -> {
                orderRepository.findById(order.getOrderId())
                        .ifPresent(order1 -> {
                            JSONObject cusReq = new JSONObject();
                            cusReq.put("Type", "CUSTOMER-ADDRESS");
                            cusReq.put("TypeId", order1.getUserAddressId());
                            customerAddress.set(dataAccessService.pickAndProcess(cusReq));
                        });
            });


        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
        }
        return customerAddress.get();
    }

    public JSONObject merchantAddress(Long itemId) {
        AtomicReference<JSONObject> merchantAddress = new AtomicReference<>(new JSONObject());
        try {
            orderItemRepository.findById(itemId).ifPresent(item -> {
                JSONObject cusReq = new JSONObject();
                cusReq.put("Type", "MERCHANT-ADDRESS");
                cusReq.put("TypeId", item.getMerchantId());
                merchantAddress.set(dataAccessService.pickAndProcess(cusReq));
            });
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
        }
        return merchantAddress.get();
    }

    public JSONObject productInfo(Long itemId) {
        AtomicReference<JSONObject> merchantAddress = new AtomicReference<>(new JSONObject());
        try {
            orderItemRepository.findById(itemId).ifPresent(item -> {
                JSONObject cusReq = new JSONObject();
                cusReq.put("Type", "PRODUCT-AND-SUB");
                cusReq.put("TypeId", item.getProductLinkingId());
                cusReq.put("SubProductId", item.getSubProductId());
                merchantAddress.set(dataAccessService.pickAndProcess(cusReq));
            });
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
        }
        return merchantAddress.get();
    }


    public JSONObject wareHouseAddress(Long itemId) {
        AtomicReference<JSONObject> wareHouseAddress = new AtomicReference<>(new JSONObject());
        try {
            JSONObject cusReq = new JSONObject();
            cusReq.put("Type", "MERCHANT-ADDRESS");
            cusReq.put("TypeId", 353);
            wareHouseAddress.set(dataAccessService.pickAndProcess(cusReq));
            wareHouseAddress.get().put("WareHouseId", 1);
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
        }
        return wareHouseAddress.get();
    }
}
