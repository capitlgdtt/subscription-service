package com.example.subscription.domain.enumerations;

import su.onno.annotations.EnumLabel;
import su.onno.annotations.Enumeration;

/**
 * Lifecycle of a {@link com.example.subscription.domain.catalogs.Customer}.
 *
 * <p>Code-controlled — the values are fixed at build time and rendered as coloured pills in
 * lists and forms. A customer's status is purely informational today (it does not gate
 * posting); the enumeration exists so that future policy changes — e.g. refusing new
 * subscriptions for blocked customers — have a natural place to hook in.</p>
 */
@Enumeration(name = "Customer Statuses", title = "Client status")
public enum CustomerStatus {
    @EnumLabel(value = "Active", color = "#059669") ACTIVE,
    @EnumLabel(value = "Blocked", color = "#DC2626") BLOCKED
}