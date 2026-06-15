package com.shopsphere.payment_Services.ServiceTest;

import com.shopsphere.payment_Services.Entity.Payment;
import com.shopsphere.payment_Services.Entity.PaymentStatus;
import com.shopsphere.payment_Services.Kafka.InventoryReleaseEvent;
import com.shopsphere.payment_Services.Kafka.InventoryReservedEvent;
import com.shopsphere.payment_Services.Kafka.PaymentFailedEvent;
import com.shopsphere.payment_Services.Kafka.PaymentEventProducer;
import com.shopsphere.payment_Services.Repository.PaymentRepository;
import com.shopsphere.payment_Services.Service.Impl.PaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Test
    void shouldProcessFailedPaymentAndPublishCompensationEvents() {

        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        InventoryReservedEvent event =
                new InventoryReservedEvent(
                        orderId,
                        5,
                        productId,
                        BigDecimal.valueOf(1000)
                );

        Payment savedPayment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .orderId(orderId)
                .amount(BigDecimal.valueOf(1000))
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(savedPayment);

        // spy so we can force the failure branch deterministically
        PaymentServiceImpl paymentService =
                spy(new PaymentServiceImpl(paymentRepository, paymentEventProducer));

        doReturn(false).when(paymentService).isPaymentSuccessful();

        paymentService.processPayment(event);

        verify(paymentRepository, times(2))
                .save(any(Payment.class));

        verify(paymentEventProducer, times(1))
                .publishFailure(any(PaymentFailedEvent.class));

        verify(paymentEventProducer, times(1))
                .publishInventoryRelease(any(InventoryReleaseEvent.class));

        ArgumentCaptor<InventoryReleaseEvent> captor =
                ArgumentCaptor.forClass(InventoryReleaseEvent.class);

        verify(paymentEventProducer)
                .publishInventoryRelease(captor.capture());

        InventoryReleaseEvent releaseEvent = captor.getValue();

        assertEquals(orderId, releaseEvent.getOrderId());
        assertEquals(productId, releaseEvent.getProductId());
        assertEquals(5, releaseEvent.getQuantity());
    }
}
