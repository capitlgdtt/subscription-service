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

/**
 * Revenue recognised by the service, sliced by tariff and customer.
 *
 * <p>A {@link AccumulationType#TURNOVER} register — it accumulates activity over a period
 * (what was sold, how much money came in) rather than carrying a running balance. A
 * {@link com.example.subscription.domain.documents.Subscription} writes one receipt per line
 * on posting; a cancelled subscription writes none.</p>
 *
 * <p><b>Denormalised names.</b> Each row carries both a {@code Ref} and a plain-string
 * {@code …Name} for the tariff and the customer. This is deliberate: register widgets on the
 * dashboard (charts, groupings) do not resolve a {@code Ref}'s display label the way entity
 * lists do, so a chart grouped by {@code tariff} would otherwise show a raw UUID. The names
 * are captured at posting time and never updated, so renaming a tariff later does not rewrite
 * historical revenue.</p>
 *
 * <p>Resources: {@code amount} (money) and {@code periods} (how many billing periods were
 * sold) — the latter lets the dashboard show period volume separately from revenue.</p>
 */
@AccumulationRegister(name = "Revenue", title = "Revenue by tariff",
        type = AccumulationType.TURNOVER, context = "Sales")
@AccessControl(readRoles = {"MANAGER", "ADMIN"})
@Getter
@Setter
public class Revenue extends AccumulationRecord {

    @Dimension(displayName = "Tariff")
    private Ref<Tariff> tariff;

    /** Snapshot of the tariff's human label at posting time — see the class Javadoc. */
    @Dimension(displayName = "Tariff name")
    private String tariffName;

    @Dimension(displayName = "Customer")
    private Ref<Customer> customer;

    /** Snapshot of the customer's human label at posting time — see the class Javadoc. */
    @Dimension(displayName = "Customer name")
    private String customerName;

    @Resource(displayName = "Amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Resource(displayName = "Periods", precision = 10, scale = 0)
    private BigDecimal periods;
}