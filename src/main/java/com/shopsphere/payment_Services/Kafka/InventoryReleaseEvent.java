package com.shopsphere.payment_Services.Kafka;

import lombok.*;

import java.util.UUID;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InventoryReleaseEvent {
    private UUID  orderId;
    private UUID  productId;
    private Integer quantity;
}
