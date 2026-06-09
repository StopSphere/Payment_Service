package com.shopsphere.payment_Services.Kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentFailedEvent
{
    private UUID orderId;

    private String reason;

    private BigDecimal amount;
}
