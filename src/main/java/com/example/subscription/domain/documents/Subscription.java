package com.example.subscription.domain.documents;

import com.example.subscription.config.ApplicationContextHolder;
import com.example.subscription.domain.catalogs.Customer;
import com.example.subscription.domain.catalogs.Tariff;
import com.example.subscription.domain.enumerations.SubscriptionStatus;
import com.example.subscription.domain.registers.AccountBalance;
import com.example.subscription.domain.registers.Revenue;
import com.example.subscription.repositories.TariffRepository;
import su.onno.annotations.AccessControl;
import su.onno.annotations.Attribute;
import su.onno.annotations.Document;
import su.onno.annotations.TabularSection;
import su.onno.lifecycle.BeforeWriteHandler;
import su.onno.lifecycle.OnFillingHandler;
import su.onno.lifecycle.Postable;
import su.onno.model.DocumentObject;
import su.onno.posting.PostingContext;
import su.onno.rules.BusinessRule;
import su.onno.rules.Validated;
import su.onno.types.Ref;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A customer's subscription to one or more {@link Tariff}s.
 *
 * <p>On posting, each line draws its amount from the customer's {@link AccountBalance} and
 * recognizes it as revenue in the {@link Revenue} turnover register, sliced by tariff and
 * customer. A subscription in {@link SubscriptionStatus#CANCELLED} posts nothing: no balance
 * movement is written and no revenue is recognized.</p>
 *
 * <p>The end date is {@code startDate + max(periodDays * periods)} across all lines — a
 * 365-day tariff and a 30-day tariff in the same document give a 365-day subscription.</p>
 */
@Document(name = "Subscriptions", title = "Subscription", numberPrefix = "S-", context = "Sales")
@AccessControl(readRoles = {"MANAGER", "ADMIN"}, writeRoles = {"MANAGER", "ADMIN"})
@Getter
@Setter
public class Subscription extends DocumentObject
        implements OnFillingHandler, BeforeWriteHandler, Validated, Postable {

    @Attribute(displayName = "Customer", required = true)
    private Ref<Customer> customer;

    @Attribute(displayName = "Status")
    private SubscriptionStatus status = SubscriptionStatus.DRAFT;

    @Attribute(displayName = "Start date", required = true)
    private LocalDate startDate;

    @Attribute(displayName = "End date")
    private LocalDate endDate;

    @Attribute(displayName = "Total", precision = 15, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Attribute(displayName = "Cancel reason", length = 500)
    private String cancelReason;

    @TabularSection(name = "lines")
    private List<SubscriptionLine> lines = new ArrayList<>();

    @Override
    public void onFilling() {
        if (getDate() == null) {
            setDate(LocalDateTime.now());
        }
        if (startDate == null) {
            startDate = getDate().toLocalDate();
        }
    }

    @Override
    public void beforeWrite() {
        TariffRepository tariffs = ApplicationContextHolder.getBean(TariffRepository.class);

        BigDecimal sum = BigDecimal.ZERO;
        int maxDurationDays = 0;

        for (SubscriptionLine line : lines) {
            int periods = line.getPeriods() == null ? 0 : line.getPeriods();
            int periodDays = 0;
            BigDecimal price = BigDecimal.ZERO;

            if (line.getTariff() != null) {
                Tariff t = tariffs.findById(line.getTariff().id()).orElse(null);
                if (t != null) {
                    price = t.getPrice() != null ? t.getPrice() : BigDecimal.ZERO;
                    line.setPrice(price);
                    periodDays = t.getPeriodDays() != null ? t.getPeriodDays() : 0;
                }
            }

            BigDecimal amount = price.multiply(BigDecimal.valueOf(periods));
            line.setAmount(amount);
            sum = sum.add(amount);

            int lineDays = periodDays * periods;
            if (lineDays > maxDurationDays) {
                maxDurationDays = lineDays;
            }
        }

        this.total = sum;

        LocalDate start = startDate != null ? startDate : LocalDate.now();
        this.endDate = start.plusDays(maxDurationDays);
    }

    @Override
    public List<BusinessRule> rules() {
        return List.of(
                new BusinessRule("customer-required", "Choose a customer",
                        () -> customer != null),
                new BusinessRule("lines-required", "Add at least one tariff line",
                        () -> status == SubscriptionStatus.CANCELLED
                                || (lines != null && !lines.isEmpty())),
                new BusinessRule("periods-positive", "Each line must have at least one period",
                        this::allPeriodsPositive),
                new BusinessRule("tariff-available", "All selected tariffs must be available",
                        this::allTariffsAvailable)
        );
    }

    private boolean allPeriodsPositive() {
        if (lines == null || lines.isEmpty()) {
            return true;
        }
        return lines.stream().allMatch(l -> l.getPeriods() != null && l.getPeriods() > 0);
    }

    private boolean allTariffsAvailable() {
        if (lines == null || lines.isEmpty()) {
            return true;
        }
        TariffRepository tariffs = ApplicationContextHolder.getBean(TariffRepository.class);
        return lines.stream().allMatch(l -> {
            if (l.getTariff() == null) {
                return false;
            }
            Tariff t = tariffs.findById(l.getTariff().id()).orElse(null);
            return t != null && t.isAvailable();
        });
    }

    @Override
    public void handlePosting(PostingContext context) {
        if (status == SubscriptionStatus.CANCELLED) {
            return;
        }

        var balance = context.movements(AccountBalance.class);
        var revenue = context.movements(Revenue.class);

        for (SubscriptionLine line : lines) {
            if (line.getTariff() == null || line.getAmount() == null) {
                continue;
            }

            balance.addExpense(r -> {
                r.setCustomer(customer);
                r.setAmount(line.getAmount());
            });

            revenue.addReceipt(r -> {
                r.setCustomer(customer);
                r.setTariff(line.getTariff());
                r.setAmount(line.getAmount());
                r.setPeriods(BigDecimal.valueOf(
                        line.getPeriods() == null ? 0 : line.getPeriods()));
            });
        }
    }
}