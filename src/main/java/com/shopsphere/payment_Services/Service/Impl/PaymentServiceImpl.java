package com.shopsphere.payment_Services.Service.Impl;

import com.shopsphere.payment_Services.Entity.Payment;
import com.shopsphere.payment_Services.Entity.PaymentStatus;
import com.shopsphere.payment_Services.Kafka.*;
import com.shopsphere.payment_Services.Repository.PaymentRepository;
import com.shopsphere.payment_Services.Service.PaymentService;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;

    @Override
    public void processPayment(InventoryReservedEvent event) {
        Payment payment =
                Payment.builder()
                        .orderId(event.getOrderId())
                        .amount(event.getAmount())
                        .status(PaymentStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build();

        payment = paymentRepository.save(payment);

        boolean success =            //intentionally simulating false
                Math.random() > 0.2;

        if(success){

            payment.setStatus(
                    PaymentStatus.SUCCESS
            );

            paymentRepository.save(payment);

            paymentEventProducer.publishSuccess(
                    new PaymentSuccessEvent(
                            payment.getOrderId(),
                            payment.getAmount(),
                            payment.getPaymentId()
                    )
            );

        }else{

            payment.setStatus(
                    PaymentStatus.FAILED
            );

            paymentRepository.save(payment);

            paymentEventProducer.publishFailure(
                    new PaymentFailedEvent(
                            payment.getOrderId(),
                            "Payment Failed",
                            payment.getAmount()
                    )
            );
            System.out.println("=== BEFORE PUBLISH ===");
            System.out.println("ORDER ID = " + event.getOrderId());
            System.out.println("PRODUCT ID = " + event.getProductId());
            System.out.println("QUANTITY = " + event.getQuantity());
            paymentEventProducer.publishInventoryRelease(
                    new InventoryReleaseEvent(
                            event.getOrderId(),
                            event.getProductId(),
                            event.getQuantity()

                    )
            );
        }
    }
}
