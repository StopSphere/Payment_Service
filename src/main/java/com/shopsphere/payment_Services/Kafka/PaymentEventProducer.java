package com.shopsphere.payment_Services.Kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String,Object>
            kafkaTemplate;

    public void publishInventoryRelease(InventoryReleaseEvent event){
        kafkaTemplate.send("inventory-release", event.getOrderId().toString(),event);

    }

    public void publishSuccess(
            PaymentSuccessEvent event){

        kafkaTemplate.send(
                "payment-success",
                event.getOrderId().toString(),
                event
        );
    }

    public void publishFailure(
            PaymentFailedEvent event){

        kafkaTemplate.send(
                "payment-failed",
                event.getOrderId().toString(),
                event
        );
    }
}
