package com.commerce.pal.payment.controller.shipping.portal;

import com.commerce.pal.payment.model.shipping.ItemShipmentStatus;
import com.commerce.pal.payment.repo.payment.OrderItemRepository;
import com.commerce.pal.payment.repo.payment.OrderRepository;
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

@Log
@CrossOrigin(origins = {"*"}, maxAge = 3600L)
@RestController
@RequestMapping({"/prime/api/v1/portal/shipping"})
@SuppressWarnings("Duplicates")
public class PortalShippingController {
    private final OrderItemRepository orderItemRepository;

    @Autowired
    public PortalShippingController(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    //Admin Configure Shipping process and the respective WareHouse
    @RequestMapping(value = "/shipping-configuration", method = RequestMethod.POST)
    public ResponseEntity<?> shippingConfiguration(@RequestBody String req) {
        JSONObject responseMap = new JSONObject();
        try {
            JSONObject request = new JSONObject(req);
            orderItemRepository.findById(request.getLong("ItemId"))
                    .ifPresentOrElse(orderItem -> {
                        orderItem.setAssignedWareHouseId(request.getInt("AssignedWareHouseId"));
                        orderItem.setShipmentType(request.getString("ShipmentType"));
                        orderItem.setFinalizingWareHouseId(request.getInt("FinalizingWareHouseId"));
                        orderItem.setShipmentTypeComments(request.getString("ShipmentTypeComments"));
                        orderItemRepository.save(orderItem);
                        responseMap.put("statusCode", ResponseCodes.REQUEST_FAILED)
                                .put("statusDescription", "Request Saved successfully")
                                .put("statusMessage", "Request Saved successfully");
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


}
