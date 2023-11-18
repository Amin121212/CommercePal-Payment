package com.commerce.pal.payment.module.payment;

import com.commerce.pal.payment.integ.payment.cash.AgentCashProcessing;
import com.commerce.pal.payment.integ.payment.ebirr.EBirrPayment;
import com.commerce.pal.payment.integ.payment.financials.FinancialPayment;
import com.commerce.pal.payment.integ.payment.hellocash.HelloCashPayment;
import com.commerce.pal.payment.integ.payment.sahay.SahayPayment;
import com.commerce.pal.payment.integ.payment.telebirr.TeleBirrPayment;
import com.commerce.pal.payment.model.payment.PalPayment;
import com.commerce.pal.payment.repo.payment.OrderRepository;
import com.commerce.pal.payment.repo.payment.PalPaymentRepository;
import com.commerce.pal.payment.util.GlobalMethods;
import com.commerce.pal.payment.util.ResponseCodes;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

@Log
@Service
@SuppressWarnings("Duplicates")
public class PaymentService {
    private final EBirrPayment eBirrPayment;
    private final SahayPayment sahayPayment;
    private final GlobalMethods globalMethods;
    private final TeleBirrPayment teleBirrPayment;
    private final OrderRepository orderRepository;
    private final FinancialPayment financialPayment;
    private final HelloCashPayment helloCashPayment;
    private final AgentCashProcessing agentCashProcessing;
    private final PalPaymentRepository palPaymentRepository;

    @Autowired
    public PaymentService(EBirrPayment eBirrPayment,
                          SahayPayment sahayPayment,
                          GlobalMethods globalMethods,
                          TeleBirrPayment teleBirrPayment,
                          OrderRepository orderRepository,
                          FinancialPayment financialPayment,
                          HelloCashPayment helloCashPayment,
                          AgentCashProcessing agentCashProcessing,
                          PalPaymentRepository palPaymentRepository) {
        this.eBirrPayment = eBirrPayment;
        this.sahayPayment = sahayPayment;
        this.globalMethods = globalMethods;
        this.teleBirrPayment = teleBirrPayment;
        this.orderRepository = orderRepository;
        this.financialPayment = financialPayment;
        this.helloCashPayment = helloCashPayment;
        this.agentCashProcessing = agentCashProcessing;
        this.palPaymentRepository = palPaymentRepository;
    }

    public JSONObject pickAndProcess(JSONObject rqBdy) {
        AtomicReference<JSONObject> respBdy = new AtomicReference<>(new JSONObject());
        try {
            orderRepository.findOrderByOrderRefAndIsUserAddressAssigned(
                            rqBdy.getString("OrderRef"), 1)
                    .ifPresentOrElse(order -> {
                        AtomicReference<PalPayment> payment = new AtomicReference<>(new PalPayment());
                        payment.get().setUserType(rqBdy.getString("UserType"));
                        payment.get().setUserEmail(rqBdy.getString("UserEmail"));
                        payment.get().setTransRef(globalMethods.generateTrans());
                        payment.get().setOrderRef(rqBdy.getString("OrderRef"));
                        payment.get().setTransType("OrderPayment");
                        payment.get().setPaymentType(rqBdy.getString("PaymentType"));
                        payment.get().setPaymentAccountType(rqBdy.getString("PaymentMode"));
                        payment.get().setAccountNumber(rqBdy.getString("UserEmail"));
                        BigDecimal amount = new BigDecimal(order.getTotalPrice().doubleValue() + order.getDeliveryPrice().doubleValue() - order.getPromotionAmount().doubleValue());
                        amount = amount.setScale(2, RoundingMode.CEILING);
                        payment.get().setAmount(amount);
                        payment.get().setCurrency(rqBdy.getString("Currency"));
                        payment.get().setStatus(0);
                        payment.get().setRequestPayload(rqBdy.toString());
                        payment.get().setRequestDate(Timestamp.from(Instant.now()));
                        payment.set(palPaymentRepository.save(payment.get()));

                        switch (rqBdy.getString("PaymentType")) {
                            case "SAHAY":
                                payment.get().setAccountNumber(rqBdy.getString("PhoneNumber"));
                                payment.set(palPaymentRepository.save(payment.get()));
                                respBdy.set(sahayPayment.pickAndProcess(payment.get()));
                                break;
                            case "HELLO-CASH":
                                payment.get().setAccountNumber(rqBdy.getString("PhoneNumber"));
                                payment.set(palPaymentRepository.save(payment.get()));
                                respBdy.set(helloCashPayment.pickAndProcess(payment.get()));
                                break;
                            case "E-BIRR":
                                payment.get().setAccountNumber(rqBdy.getString("PhoneNumber"));
                                payment.set(palPaymentRepository.save(payment.get()));
                                respBdy.set(eBirrPayment.pickAndProcess(payment.get()));
                                break;
                            case "TELE-BIRR":
                                payment.get().setAccountNumber(rqBdy.getString("PhoneNumber"));
                                payment.set(palPaymentRepository.save(payment.get()));
                                respBdy.set(teleBirrPayment.pickAndProcess(payment.get()));
                                break;
                            case "FINANCE-INST":
                                respBdy.set(financialPayment.pickAndProcess(payment.get()));
                                break;
                            case "AGENT-CASH":
                                payment.get().setAccountNumber(rqBdy.getString("PhoneNumber"));
                                payment.set(palPaymentRepository.save(payment.get()));
                                respBdy.set(agentCashProcessing.pickAndProcess(payment.get()));
                                break;
                            default:
                                respBdy.get().put("statusCode", ResponseCodes.SYSTEM_ERROR)
                                        .put("statusDescription", "failed")
                                        .put("statusMessage", "Request failed");
                                break;
                        }
                    }, () -> {
                        respBdy.get().put("statusCode", ResponseCodes.SYSTEM_ERROR)
                                .put("statusDescription", "Order has to be assigned shipment address")
                                .put("statusMessage", "Order has to be assigned shipment address");
                    });
            //payment.set
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
            respBdy.get().put("statusCode", ResponseCodes.SYSTEM_ERROR)
                    .put("statusDescription", "failed")
                    .put("statusMessage", "Request failed");
        }
        return respBdy.get();
    }
}
