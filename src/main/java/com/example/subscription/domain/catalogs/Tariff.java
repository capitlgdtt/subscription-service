package com.example.subscription.domain.catalogs;

import su.onno.annotations.AccessControl;
import su.onno.annotations.Attribute;
import su.onno.annotations.Catalog;
import su.onno.model.CatalogObject;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

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