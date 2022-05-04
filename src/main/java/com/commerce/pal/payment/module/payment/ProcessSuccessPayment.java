package com.commerce.pal.payment.module.payment;

import com.commerce.pal.payment.model.payment.PalPayment;
import com.commerce.pal.payment.repo.payment.OrderItemRepository;
import com.commerce.pal.payment.repo.payment.OrderRepository;
import com.commerce.pal.payment.repo.payment.PalPaymentRepository;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Level;

import static com.commerce.pal.payment.util.TransactionStatus.PAYMENT_SUCCESS;

@Log
@Service
public class ProcessSuccessPayment {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PalPaymentRepository palPaymentRepository;

    @Autowired
    public ProcessSuccessPayment(OrderRepository orderRepository,
                                 OrderItemRepository orderItemRepository,
                                 PalPaymentRepository palPaymentRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.palPaymentRepository = palPaymentRepository;
    }

    @Async
    public void pickAndProcess(PalPayment payment) {
        try {
            orderRepository.findOrderByOrderRef(payment.getOrderRef())
                    .ifPresent(order -> {
                        order.setStatus(PAYMENT_SUCCESS);
                        order.setStatusDescription("Payment Successful");
                        order.setBillerReference(payment.getBillTransRef());
                        order.setPaymentDate(Timestamp.from(Instant.now()));
                        order.setPaymentStatus(PAYMENT_SUCCESS);
                        orderRepository.save(order);
                        orderItemRepository.findOrderItemsByOrderId(order.getOrderId())
                                .forEach(orderItem -> {
                                    orderItem.setStatus(PAYMENT_SUCCESS);
                                    orderItem.setStatusDescription("Payment Successful");
                                    orderItemRepository.save(orderItem);
                                });


                    });
        } catch (Exception ex) {
            log.log(Level.WARNING, ex.getMessage());
        }
    }
}
