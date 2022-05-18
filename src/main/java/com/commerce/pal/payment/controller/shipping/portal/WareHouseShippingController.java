package com.commerce.pal.payment.controller.shipping.portal;

import com.commerce.pal.payment.model.shipping.ItemShipmentStatus;
import com.commerce.pal.payment.repo.payment.OrderItemRepository;
import com.commerce.pal.payment.repo.shipping.ItemShipmentStatusRepository;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Level;

import static com.commerce.pal.payment.util.StatusCodes.AcceptReadyForPickUp;
import static com.commerce.pal.payment.util.StatusCodes.AssignMessengerAndDelivery;

@Log
@CrossOrigin(origins = {"*"}, maxAge = 3600L)
@RestController
@RequestMapping({"/prime/api/v1/ware-house/shipping"})
@SuppressWarnings("Duplicates")
public class WareHouseShippingController {
    private final OrderItemRepository orderItemRepository;
    private final ItemShipmentStatusRepository itemShipmentStatusRepository;

    @Autowired
    public WareHouseShippingController(OrderItemRepository orderItemRepository,
                                       ItemShipmentStatusRepository itemShipmentStatusRepository) {
        this.orderItemRepository = orderItemRepository;
        this.itemShipmentStatusRepository = itemShipmentStatusRepository;
    }

    // Assign the Items to Specific WareHouse
    // Filter Paid orders by status
    @RequestMapping(value = "/warehouse-assign-messenger", method = RequestMethod.POST)
    public ResponseEntity<?> warehouseAssignMessenger(@RequestBody String req) {
        JSONObject responseMap = new JSONObject();
        try {

            JSONObject request = new JSONObject(req);
            orderItemRepository.findById(
                            request.getLong("ItemId"))
                    .ifPresentOrElse(orderItem -> {
                        orderItem.setShipmentStatus(AssignMessengerAndDelivery);
                        orderItem.setShipmentUpdateDate(Timestamp.from(Instant.now()));

                        ItemShipmentStatus itemShipmentStatus = new ItemShipmentStatus();
                        itemShipmentStatus.setItemId(orderItem.getItemId());
                        itemShipmentStatus.setShipmentStatus(AssignMessengerAndDelivery);
                        itemShipmentStatus.setComments(request.getString("Comments"));
                        itemShipmentStatus.setStatus(1);
                        itemShipmentStatus.setCreatedDate(Timestamp.from(Instant.now()));
                        itemShipmentStatusRepository.save(itemShipmentStatus);
                        // Send Notification of Acceptance
                        //merchantAcceptAndPickUpNotification.pickAndProcess(orderItem.getOrderId().toString());


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
