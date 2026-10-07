package com.example.subscription.domain.registers;

import com.example.subscription.domain.catalogs.Customer;
import com.example.subscription.domain.catalogs.Tariff;
import su.onno.annotations.AccumulationRegister;
import su.onno.annotations.AccessControl;
import su.onno.annotations.Dimension;
import su.onno.annotations.Resource;
import su.onno.model.AccumulationRecord;
import su.onno.model.AccumulationType;
import su.onno.types.Ref;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@AccumulationRegister(name = "Revenue", title = "Revenue by tariff",
        type = AccumulationType.TURNOVER, context = "Sales")
@AccessControl(readRoles = {"MANAGER", "ADMIN"})
@Getter
@Setter
public class Revenue extends AccumulationRecord {

    @Dimension(displayName = "Tariff")
    private Ref<Tariff> tariff;

    @Dimension(displayName = "Customer")
    private Ref<Customer> customer;

    @Resource(displayName = "Amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Resource(displayName = "Periods", precision = 10, scale = 0)
    private BigDecimal periods;
}