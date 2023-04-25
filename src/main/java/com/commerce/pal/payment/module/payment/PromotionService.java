package com.commerce.pal.payment.module.payment;

import com.commerce.pal.payment.model.payment.PalPayment;
import com.commerce.pal.payment.repo.payment.OrderRepository;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.PersistenceContext;
import javax.persistence.StoredProcedureQuery;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

@Log
@Service
@SuppressWarnings("Duplicates")
public class PromotionService {
    @PersistenceContext
    private EntityManager entityManager;

    private final OrderRepository orderRepository;

    public PromotionService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public JSONObject pickAndProcess(JSONObject rqBdy) {
        AtomicReference<JSONObject> respBdy = new AtomicReference<>(new JSONObject());
        try {
            orderRepository.findOrderByOrderRefAndIsUserAddressAssigned(
                    rqBdy.getString("OrderRef"), 1)
                    .ifPresentOrElse(order -> {
                        JSONObject promoBody = new JSONObject();
                        promoBody.put("PromoCode", rqBdy.getString("PromoCode"));
                        promoBody.put("OrderId", order.getOrderId().toString());
                        promoBody.put("UserId", String.valueOf(rqBdy.getLong("UserId")));
                        promoBody.put("SaleType", order.getSaleType());

                        JSONObject promoRes = getExecPromotion(promoBody);
                        if (promoRes.getString("Status").equals("00")) {
                            if (promoRes.getString("PromoStatus").equals("3")) {
                                order.setPromotionId(Integer.valueOf(promoRes.getString("PromotionId")));
                                order.setPromotionAmount(new BigDecimal(promoRes.getString("PromotionAmount")));
                                orderRepository.save(order);

                                respBdy.get().put("statusCode", ResponseCodes.SUCCESS)
                                        .put("PromotionAmount", promoRes.getString("PromotionAmount"))
                                        .put("statusDescription", "Success")
                                        .put("statusMessage", "Success");

                            } else {
                                respBdy.get().put("statusCode", ResponseCodes.SYSTEM_ERROR)
                                        .put("statusDescription", promoRes.getString("PromoMessage"))
                                        .put("statusMessage", promoRes.getString("PromoMessage"));
                            }
                        }
                    }, () -> {
                        respBdy.get().put("statusCode", ResponseCodes.SYSTEM_ERROR)
                                .put("statusDescription", "Order has to be assigned shipment address")
                                .put("statusMessage", "Order has to be assigned shipment address");
                    });
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
            respBdy.get().put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed")
                    .put("statusMessage", "Request failed");
        }
        return respBdy.get();
    }

    public JSONObject getExecPromotion(JSONObject reqBody) {
        JSONObject transResponse = new JSONObject();
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("GetExecPromotion");
            query.registerStoredProcedureParameter("PromoCode", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("OrderId", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("UserId", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("SaleType", String.class, ParameterMode.IN);

            query.setParameter("PromoCode", reqBody.getString("PromoCode"));
            query.setParameter("OrderId", reqBody.getString("OrderId"));
            query.setParameter("UserId", reqBody.getString("UserId"));
            query.setParameter("SaleType", reqBody.getString("SaleType"));

             /*
            OUTPUT PARAMS
             */
            query.registerStoredProcedureParameter("PromoStatus", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("PromoMessage", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("PromotionId", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("PromotionAmount", String.class, ParameterMode.OUT);

            query.execute();

            transResponse.put("PromoStatus", query.getOutputParameterValue("PromoStatus"));
            transResponse.put("PromoMessage", query.getOutputParameterValue("PromoMessage"));
            transResponse.put("PromotionId", query.getOutputParameterValue("PromotionId"));
            transResponse.put("PromotionAmount", query.getOutputParameterValue("PromotionAmount"));

            transResponse.put("Status", "00");
            transResponse.put("Message", "The request was processed successfully");
        } catch (Exception ex) {
            log.log(Level.WARNING, "processOrderPayment CLASS : " + ex.getMessage());
            transResponse.put("Status", "101");
            transResponse.put("Message", "Failed while processing the request");
        }
        return transResponse;

    }
}
