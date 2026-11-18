package com.paybank.account.model;

public enum MovementType {
    /** Money leaving the account (the user paid). */
    CHARGE,
    /** Money entering the account (the user received a payment). */
    CREDIT
}
