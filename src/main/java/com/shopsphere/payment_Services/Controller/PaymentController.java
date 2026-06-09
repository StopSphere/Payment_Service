package com.shopsphere.payment_Services.Controller;

import com.shopsphere.payment_Services.Service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;



}
