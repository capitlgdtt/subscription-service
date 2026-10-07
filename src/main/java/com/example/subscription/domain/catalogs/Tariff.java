package com.example.subscription.domain.catalogs;

import su.onno.annotations.AccessControl;
import su.onno.annotations.Attribute;
import su.onno.annotations.Catalog;
import su.onno.model.CatalogObject;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A subscription plan: a price, a billing period, and whether it can currently be purchased.
 *
 * <p>Master data, so a manager can raise prices, extend periods, or retire a plan without
 * a deployment. Both the price and the period length feed into every
 * {@link com.example.subscription.domain.documents.SubscriptionLine} that references this
 * tariff:</p>
 * <ul>
 *   <li>{@code price} is copied onto the line and drives both the document total and the
 *       amount posted to the revenue register;</li>
 *   <li>{@code periodDays} × the line's period count contributes to the subscription's
 *       end date (the maximum across lines, not the sum);</li>
 *   <li>{@code available} gates the "can this tariff be subscribed to" business rule — a
 *       retired plan can still be referenced by historical subscriptions, but cannot be
 *       picked on a new one.</li>
 * </ul>
 */
@Catalog(name = "Tariffs", title = "Tariff", codePrefix = "T-", context = "Sales")
@AccessControl(readRoles = {"MANAGER", "ADMIN"}, writeRoles = {"MANAGER", "ADMIN"})
@Getter
@Setter
public class Tariff extends CatalogObject {

    @Attribute(displayName = "Price per period", precision = 14, scale = 2, required = true)
    private BigDecimal price = BigDecimal.ZERO;

    @Attribute(displayName = "Period length (days)", precision = 10, scale = 0, required = true)
    private Integer periodDays = 30;

    @Attribute(displayName = "Available for subscription")
    private boolean available = true;
}