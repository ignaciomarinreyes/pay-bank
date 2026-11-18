package com.paybank.account.mapper;

import java.time.ZoneOffset;

import com.paybank.account.model.MovementType;
import com.paybank.account.repository.MovementRow;
import com.paybank.generated.model.MovementResponse;
import org.springframework.stereotype.Component;

@Component
public class MovementMapper {

    /** The amount is exposed with sign: negative for a charge, positive for a credit. */
    public MovementResponse toResponse(MovementRow row) {
        var amount = row.type() == MovementType.CHARGE ? row.amount().negate() : row.amount();
        return new MovementResponse(
                row.id(),
                row.createdAt().atOffset(ZoneOffset.UTC),
                MovementResponse.TypeEnum.valueOf(row.type().name()),
                amount,
                row.balanceAfter(),
                row.counterpartyAlias(),
                row.concept());
    }
}
