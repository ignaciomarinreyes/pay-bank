package com.paybank.account.repository;

import java.math.BigDecimal;
import java.time.Instant;

import com.paybank.account.model.MovementType;

/** Read model of a movement with its counterparty resolved. */
public record MovementRow(
        Long id,
        Instant createdAt,
        MovementType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String concept,
        String counterpartyAlias) {
}
