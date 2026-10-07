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

/**
 * Running balance of every customer's account with the service.
 *
 * <p>A {@link AccumulationType#BALANCE} register — it stores a current total per customer,
 * not period activity. A {@link com.example.subscription.domain.documents.Payment} writes a
 * receipt (money in); a {@link com.example.subscription.domain.documents.Subscription} writes
 * an expense (money out) on posting.</p>
 *
 * <p><b>{@code allowNegative = false} is a load-bearing declaration.</b> It is what makes the
 * "cannot subscribe without enough money" rule hold: the posting engine refuses any movement
 * that would drive a customer's balance below zero, so an underfunded subscription cannot be
 * posted. There is no hand-written check for this in the subscription document — the guarantee
 * comes entirely from the register's declaration.</p>
 *
 * <p>Sliced by {@code customer} (a {@link Dimension}); the accumulated number is
 * {@code amount} (a {@link Resource}).</p>
 */
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