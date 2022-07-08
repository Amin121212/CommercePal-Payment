package com.commerce.pal.payment.module.payment;

import com.commerce.pal.payment.model.payment.PalPayment;
import com.commerce.pal.payment.model.shipping.ItemShipmentStatus;
import com.commerce.pal.payment.module.payment.store.PaymentStoreProcedure;
import com.commerce.pal.payment.module.shipping.notification.process.OrderPaymentNotification;
import com.commerce.pal.payment.repo.payment.OrderItemRepository;
import com.commerce.pal.payment.repo.payment.OrderRepository;
import com.commerce.pal.payment.repo.payment.PalPaymentRepository;
import com.commerce.pal.payment.repo.shipping.ItemShipmentStatusRepository;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Level;

import static com.commerce.pal.payment.util.StatusCodes.NewOrder;
import static com.commerce.pal.payment.util.TransactionStatus.PAYMENT_SUCCESS;

@Log
@Service
public class ProcessSuccessPayment {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PalPaymentRepository palPaymentRepository;
    private final PaymentStoreProcedure paymentStoreProcedure;
    private final OrderPaymentNotification orderPaymentNotification;


    @Autowired
    public ProcessSuccessPayment(OrderRepository orderRepository,
                                 OrderItemRepository orderItemRepository,
                                 PalPaymentRepository palPaymentRepository,
                                 PaymentStoreProcedure paymentStoreProcedure,
                                 OrderPaymentNotification orderPaymentNotification) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.palPaymentRepository = palPaymentRepository;
        this.paymentStoreProcedure = paymentStoreProcedure;
        this.orderPaymentNotification = orderPaymentNotification;
    }

    @Async
    public void pickAndProcess(PalPayment payment) {
        try {
            //Process Payment SP
            orderRepository.findOrderByOrderRef(payment.getOrderRef())
                    .ifPresent(order -> {
                        order.setStatus(PAYMENT_SUCCESS);
                        order.setStatusDescription("Payment Successful");
                        order.setBillerReference(payment.getBillTransRef());
                        order.setPaymentMethod(payment.getPaymentType());
                        order.setPaymentDate(Timestamp.from(Instant.now()));
                        order.setPaymentStatus(PAYMENT_SUCCESS);
                        order.setShippingStatus("NEW");
                        orderRepository.save(order);
                        orderItemRepository.findOrderItemsByOrderId(order.getOrderId())
                                .forEach(orderItem -> {
                                    orderItem.setShipmentStatus(NewOrder);
                                    orderItem.setShipmentType("");
                                    orderItem.setShipmentUpdateDate(Timestamp.from(Instant.now()));
                                    orderItem.setStatus(PAYMENT_SUCCESS);
                                    orderItem.setStatusDescription("Payment Successful");
                                    orderItemRepository.save(orderItem);
                                });
                        JSONObject orderPayment = new JSONObject();
                        BigDecimal orderAmount = new BigDecimal(order.getTotalPrice().doubleValue() - order.getTax().doubleValue() - order.getTax().doubleValue());
                        orderPayment.put("TransRef", order.getOrderRef());
                        orderPayment.put("PaymentType", payment.getPaymentType());
                        orderPayment.put("PaymentAccountType", payment.getPaymentAccountType());
                        orderPayment.put("CountryCode", order.getCountryCode());
                        orderPayment.put("Currency", order.getCurrency());
                        orderPayment.put("TotalAmount", order.getTotalPrice().toString());
                        orderPayment.put("OrderPayment", orderAmount.toString());
                        orderPayment.put("TaxAmount", order.getTax().toString());
                        orderPayment.put("DeliveryAmount", order.getDeliveryPrice().toString());
                        orderPayment.put("PaymentNarration", "Payment for Order Ref : " + order.getOrderRef());
                        JSONObject payRes = paymentStoreProcedure.processOrderPayment(orderPayment);
                        log.log(Level.INFO, "Payment Res : " + payRes.toString());
                        // SEND NOTIFICATIONS FOR PAYMENT SUCCESS AND SHIPPING STATUS
                        orderPaymentNotification.pickAndProcess(order.getOrderRef());
                    });
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
        }
    }
}
