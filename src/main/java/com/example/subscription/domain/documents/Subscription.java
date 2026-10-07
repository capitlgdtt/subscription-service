package com.example.subscription.domain.documents;

import com.example.subscription.config.ApplicationContextHolder;
import com.example.subscription.domain.catalogs.Customer;
import com.example.subscription.domain.catalogs.Tariff;
import com.example.subscription.domain.enumerations.SubscriptionStatus;
import com.example.subscription.domain.registers.AccountBalance;
import com.example.subscription.domain.registers.Revenue;
import com.example.subscription.repositories.CustomerRepository;
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
 * A customer's subscription to one or more {@link Tariff}s — the central document of the
 * service.
 *
 * <p>Each line is a tariff purchased for N periods. Posting draws every line's amount from
 * the customer's {@link AccountBalance} and recognises it as revenue in the {@link Revenue}
 * turnover register, sliced by tariff and customer. A subscription in
 * {@link SubscriptionStatus#CANCELLED} posts nothing: no balance movement is written and no
 * revenue is recognised — the document survives only as an audit record with a reason.</p>
 *
 * <p><b>Key invariants.</b></p>
 * <ul>
 *   <li>{@code total} is the sum of line amounts, recomputed on every write by
 *       {@link #beforeWrite()}.</li>
 *   <li>{@code endDate} is {@code startDate} plus the <b>maximum</b>
 *       {@code periodDays × periods} across lines — not the sum. A 365-day tariff and a
 *       30-day tariff in the same document give a 365-day subscription.</li>
 *   <li>A line's price and amount are re-read from its tariff on every write, so a tariff's
 *       current price always drives the total of a not-yet-posted document. Once a document
 *       has posted, the balance and revenue movements it wrote are frozen — a later tariff
 *       price change cannot retroactively rewrite the register.</li>
 * </ul>
 *
 * <p><b>Underfunded subscriptions.</b> There is no hand-written "is there enough money"
 * check here. The guarantee comes from declaring {@link AccountBalance} a BALANCE register
 * with {@code allowNegative = false}: the posting engine refuses any posting that would drive
 * a customer's balance below zero. If a subscription cannot be paid for, the post fails and
 * nothing is persisted — neither balance nor revenue.</p>
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

    /**
     * Pre-fills {@code date} and {@code startDate} on the first save of a new document.
     * Both assignments are guarded: onFilling runs on every save of a <em>new</em> entity,
     * so an explicitly set value (from a seeder, an importer, or the user) must survive.
     */
    @Override
    public void onFilling() {
        if (getDate() == null) {
            setDate(LocalDateTime.now());
        }
        if (startDate == null) {
            startDate = getDate().toLocalDate();
        }
    }

    /**
     * Recomputes derived fields from the current set of lines: each line's {@code price}
     * (re-read from its tariff) and {@code amount}, the document {@code total}, and the
     * {@code endDate}.
     *
     * <p>Called automatically by the onno write pipeline on every save, and again by the
     * posting engine before it writes movements — so a subscription posted immediately after
     * being created is guaranteed to have consistent derived fields even if the caller never
     * invoked this method directly.</p>
     */
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

            // The document lasts as long as its longest line, not the sum of all lines:
            // a 365-day tariff and a 30-day tariff overlap, they do not stack.
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
                // A cancelled subscription posts nothing and only needs a reason — it is an
                // audit artefact, not a live commercial document, so it may carry zero lines.
                new BusinessRule("lines-required", "Add at least one tariff line",
                        () -> status == SubscriptionStatus.CANCELLED
                                || (lines != null && !lines.isEmpty())),
                new BusinessRule("periods-positive", "Each line must have at least one period",
                        this::allPeriodsPositive),
                // A retired tariff can still be referenced by a historical document, but must
                // not be selectable on a new one.
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

    /**
     * Writes one balance expense and one revenue receipt per line.
     *
     * <p>The tariff and customer <em>names</em> are copied onto the revenue record alongside
     * their references. Register widgets (charts, groupings) do not resolve a {@code Ref}'s
     * display value the way entity lists do, so the denormalised strings are what the
     * dashboard actually renders. They are written at posting time and never touched again,
     * so a later rename of a tariff or customer does not rewrite history.</p>
     */
    @Override
    public void handlePosting(PostingContext context) {
        // A cancelled subscription is a terminal audit record: no movements, no revenue.
        if (status == SubscriptionStatus.CANCELLED) {
            return;
        }

        TariffRepository tariffRepo = ApplicationContextHolder.getBean(TariffRepository.class);
        CustomerRepository customerRepo = ApplicationContextHolder.getBean(CustomerRepository.class);

        String customerName = customerRepo.findById(customer.id())
                .map(c -> c.getDescription() != null ? c.getDescription() : c.getCode())
                .orElse(null);

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
                r.setCustomerName(customerName);
                r.setTariff(line.getTariff());
                r.setTariffName(tariffRepo.findById(line.getTariff().id())
                        .map(t -> t.getDescription() != null ? t.getDescription() : t.getCode())
                        .orElse(null));
                r.setAmount(line.getAmount());
                r.setPeriods(BigDecimal.valueOf(
                        line.getPeriods() == null ? 0 : line.getPeriods()));
            });
        }
    }
}