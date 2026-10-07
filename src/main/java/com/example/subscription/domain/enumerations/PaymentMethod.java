package com.example.subscription.domain.enumerations;

import su.onno.annotations.EnumLabel;
import su.onno.annotations.Enumeration;

/**
 * How a {@link com.example.subscription.domain.documents.Payment} was made.
 *
 * <p>Recorded for reporting — the dashboard groups payments by this value — but does not
 * affect posting: whatever the method, a posted payment is a single receipt into the
 * customer's balance register.</p>
 */
@Enumeration(name = "Payment Methods", title = "Payment method")
public enum PaymentMethod {
    @EnumLabel(value = "Card", color = "#2563EB") CARD,
    @EnumLabel(value = "Bank transfer", color = "#7C3AED") BANK_TRANSFER,
    @EnumLabel(value = "Cash", color = "#059669") CASH
}