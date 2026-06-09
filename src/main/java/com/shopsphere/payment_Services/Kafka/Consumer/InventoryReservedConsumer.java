package com.shopsphere.payment_Services.Kafka.Consumer;

import com.shopsphere.payment_Services.Kafka.InventoryReservedEvent;
import com.shopsphere.payment_Services.Service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryReservedConsumer {
    private final PaymentService paymentService;

    @KafkaListener(topics = "inventory-reserved", groupId = "payment-service-group")
    public void consume(InventoryReservedEvent event){
        paymentService.processPayment(event);
    }
}
