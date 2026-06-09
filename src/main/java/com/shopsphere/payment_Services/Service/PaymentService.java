package com.shopsphere.payment_Services.Service;

import com.shopsphere.payment_Services.Kafka.InventoryReservedEvent;

public interface PaymentService {

    void processPayment(InventoryReservedEvent event);


}
