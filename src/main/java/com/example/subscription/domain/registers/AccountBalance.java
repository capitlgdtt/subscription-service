package com.example.subscription.domain.registers;

import com.example.subscription.domain.catalogs.Customer;
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

@AccumulationRegister(name = "Account Balances", title = "Account balance",
        type = AccumulationType.BALANCE, allowNegative = false, context = "Sales")
@AccessControl(readRoles = {"MANAGER", "ADMIN"})
@Getter
@Setter
public class AccountBalance extends AccumulationRecord {

    @Dimension(displayName = "Customer")
    private Ref<Customer> customer;

    @Resource(displayName = "Amount", precision = 15, scale = 2)
    private BigDecimal amount;
}