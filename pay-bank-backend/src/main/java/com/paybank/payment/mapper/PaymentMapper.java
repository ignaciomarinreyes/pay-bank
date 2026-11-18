package com.paybank.payment.mapper;

import java.time.ZoneOffset;

import com.paybank.generated.model.PaymentResponse;
import com.paybank.payment.service.PaymentService;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(PaymentService.Result result) {
        return new PaymentResponse(
                result.id(),
                result.recipientAlias(),
                result.amount(),
                result.concept(),
                result.createdAt().atOffset(ZoneOffset.UTC),
                result.balanceAfter());
    }
}
