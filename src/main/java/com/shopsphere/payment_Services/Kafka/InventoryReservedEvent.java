package com.shopsphere.payment_Services.Kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryReservedEvent {

    private UUID orderId;
    private Integer quantity;
    private UUID productId;
    private BigDecimal amount;

}
