package com.paybank.payment.controller;

import com.paybank.generated.api.PaymentsApi;
import com.paybank.generated.model.CreatePaymentRequest;
import com.paybank.generated.model.PaymentResponse;
import com.paybank.payment.mapper.PaymentMapper;
import com.paybank.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController implements PaymentsApi {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    public PaymentController(PaymentService paymentService, PaymentMapper paymentMapper) {
        this.paymentService = paymentService;
        this.paymentMapper = paymentMapper;
    }

    @Override
    public ResponseEntity<PaymentResponse> createPayment(CreatePaymentRequest request) {
        var result = paymentService.createPayment(request.getRecipientAlias(), request.getAmount(), request.getConcept());
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentMapper.toResponse(result));
    }
}
