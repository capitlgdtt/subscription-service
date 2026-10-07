package com.example.subscription.domain.enumerations;

import su.onno.annotations.EnumLabel;
import su.onno.annotations.Enumeration;

@Enumeration(name = "Customer Statuses", title = "Client status")
public enum CustomerStatus {
    @EnumLabel(value = "Active", color = "#059669") ACTIVE,
    @EnumLabel(value = "Blocked", color = "#DC2626") BLOCKED
}