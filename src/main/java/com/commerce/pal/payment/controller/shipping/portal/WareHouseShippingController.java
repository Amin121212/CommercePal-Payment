package com.commerce.pal.payment.controller.shipping.portal;

import com.commerce.pal.payment.model.shipping.ItemMessengerDelivery;
import com.commerce.pal.payment.model.shipping.ItemShipmentStatus;
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
    private final ItemMessengerDeliveryRepository itemMessengerDeliveryRepository;

    @Autowired
    public WareHouseShippingController(GlobalMethods globalMethods,
                                       OrderRepository orderRepository,
                                       OrderItemRepository orderItemRepository,
                                       ItemShipmentStatusRepository itemShipmentStatusRepository,
                                       ItemMessengerDeliveryRepository itemMessengerDeliveryRepository) {
        this.globalMethods = globalMethods;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.itemShipmentStatusRepository = itemShipmentStatusRepository;
        this.itemMessengerDeliveryRepository = itemMessengerDeliveryRepository;
    }

    // Assign the Items to Specific WareHouse
    // Filter Paid orders by status
    @RequestMapping(value = "/assign-messenger-pick-merchant", method = RequestMethod.POST)
    public ResponseEntity<?> assignMessengerPickMerchant(@RequestBody String req) {
        JSONObject responseMap = new JSONObject();
        try {

            JSONObject request = new JSONObject(req);
            orderItemRepository.findById(
                            request.getLong("ItemId"))
                    .ifPresentOrElse(orderItem -> {
                        orderItem.setShipmentStatus(AssignMessengerPickAtMerchant);
                        orderItem.setShipmentUpdateDate(Timestamp.from(Instant.now()));

                        ItemShipmentStatus itemShipmentStatus = new ItemShipmentStatus();
                        itemShipmentStatus.setItemId(orderItem.getItemId());
                        itemShipmentStatus.setShipmentStatus(AssignMessengerPickAtMerchant);
                        itemShipmentStatus.setComments(request.getString("Comments"));
                        itemShipmentStatus.setStatus(1);
                        itemShipmentStatus.setCreatedDate(Timestamp.from(Instant.now()));
                        itemShipmentStatusRepository.save(itemShipmentStatus);

                        ItemMessengerDelivery itemMessengerDelivery = new ItemMessengerDelivery();
                        itemMessengerDelivery.setOrderItemId(orderItem.getItemId());
                        itemMessengerDelivery.setDeliveryType(request.getString("DeliveryType"));
                        itemMessengerDelivery.setMessengerId(request.getLong("MessengerId"));
                        itemMessengerDelivery.setMerchantId(0l);
                        itemMessengerDelivery.setWareHouseId(0l);
                        itemMessengerDelivery.setCustomerId(0l);
                        switch (request.getString("DeliveryType")) {
                            case "MC":
                                itemMessengerDelivery.setMessengerId(orderItem.getMerchantId());
                                itemMessengerDelivery.setCustomerId(orderRepository.findById(orderItem.getItemId()).get().getCustomerId());
                                break;
                            case "MW":
                                itemMessengerDelivery.setMessengerId(orderItem.getMerchantId());
                                itemMessengerDelivery.setWareHouseId(request.getLong("WareHouseId"));
                                break;
                            case "WC":
                                itemMessengerDelivery.setWareHouseId(request.getLong("WareHouseId"));
                                itemMessengerDelivery.setCustomerId(orderRepository.findById(orderItem.getItemId()).get().getCustomerId());
                                break;
                        }
                        String validationCode = globalMethods.generateValidationCode();
                        itemMessengerDelivery.setValidationCode(globalMethods.encryptCode(validationCode));
                        itemMessengerDelivery.setValidationStatus(0);
                        itemMessengerDelivery.setStatus(0);
                        itemMessengerDelivery.setCreatedDate(Timestamp.from(Instant.now()));
                        itemMessengerDeliveryRepository.save(itemMessengerDelivery);
                        // Send Notification to Respective Users
                        //merchantAcceptAndPickUpNotification.pickAndProcess(orderItem.getOrderId().toString());
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
