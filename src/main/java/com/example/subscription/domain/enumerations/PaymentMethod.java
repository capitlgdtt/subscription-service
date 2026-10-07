package com.example.subscription.domain.enumerations;

import su.onno.annotations.EnumLabel;
import su.onno.annotations.Enumeration;

@Enumeration(name = "Payment Methods", title = "Payment method")
public enum PaymentMethod {
    @EnumLabel(value = "Card", color = "#2563EB") CARD,
    @EnumLabel(value = "Bank transfer", color = "#7C3AED") BANK_TRANSFER,
    @EnumLabel(value = "Cash", color = "#059669") CASH
}