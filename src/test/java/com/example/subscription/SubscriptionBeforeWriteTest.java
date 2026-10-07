package com.example.subscription;

import com.example.subscription.domain.catalogs.Customer;
import com.example.subscription.domain.catalogs.Tariff;
import com.example.subscription.domain.documents.Subscription;
import com.example.subscription.domain.documents.SubscriptionLine;
import com.example.subscription.domain.enumerations.SubscriptionStatus;
import com.example.subscription.repositories.CustomerRepository;
import com.example.subscription.repositories.SubscriptionRepository;
import com.example.subscription.repositories.TariffRepository;
import su.onno.types.Ref;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the auto-fill and recompute contract of {@link Subscription#beforeWrite()}.
 *
 * <p>{@code beforeWrite} is invoked automatically by {@code WriteLifecycle} when a document is
 * saved through the UI command service, and by {@code PostingEngine} when a document is posted.
 * The Spring Data JDBC repository path does <b>not</b> invoke it — so the test calls the hook
 * directly to exercise the rule in isolation from any write pipeline.</p>
 */
class SubscriptionBeforeWriteTest extends AbstractIntegrationTest {

    @Autowired CustomerRepository customers;
    @Autowired TariffRepository tariffs;
    @Autowired SubscriptionRepository subscriptions;

    @Test
    void totalAndEndDate_areComputedFromLines() {
        Customer customer = newCustomer("Acme");
        Tariff monthly = newTariff("Monthly", "10.00", 30);
        Tariff yearly = newTariff("Yearly", "100.00", 365);

        Subscription sub = new Subscription();
        sub.setCustomer(Ref.of(Customer.class, customer.getId()));
        sub.setStatus(SubscriptionStatus.DRAFT);
        sub.setStartDate(LocalDate.of(2026, 1, 1));

        sub.getLines().add(line(monthly, 1));   // 10.00, 30 days
        sub.getLines().add(line(yearly, 1));    // 100.00, 365 days

        sub.beforeWrite();

        // Total = sum of all line amounts.
        assertThat(sub.getTotal()).isEqualByComparingTo("110.00");

        // End date uses the maximum line duration (365 days), not the sum (395 days).
        assertThat(sub.getEndDate()).isEqualTo(LocalDate.of(2026, 1, 1).plusDays(365));

        // Each line has its amount recomputed from tariff price × periods.
        assertThat(sub.getLines().get(0).getAmount()).isEqualByComparingTo("10.00");
        assertThat(sub.getLines().get(0).getPrice()).isEqualByComparingTo("10.00");
        assertThat(sub.getLines().get(1).getAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void multiplePeriods_multiplyAmount_andExtendDuration() {
        Customer customer = newCustomer("Acme");
        Tariff monthly = newTariff("Monthly", "10.00", 30);

        Subscription sub = new Subscription();
        sub.setCustomer(Ref.of(Customer.class, customer.getId()));
        sub.setStatus(SubscriptionStatus.DRAFT);
        sub.setStartDate(LocalDate.of(2026, 1, 1));

        sub.getLines().add(line(monthly, 12));  // 12 × 10 = 120, 12 × 30 = 360 days

        sub.beforeWrite();

        assertThat(sub.getTotal()).isEqualByComparingTo("120.00");
        assertThat(sub.getEndDate()).isEqualTo(LocalDate.of(2026, 1, 1).plusDays(360));
    }

    // --- helpers -------------------------------------------------------------------------------

    private Customer newCustomer(String name) {
        Customer c = new Customer();
        c.setDescription(name);
        return customers.save(c);
    }

    private Tariff newTariff(String name, String price, int periodDays) {
        Tariff t = new Tariff();
        t.setDescription(name);
        t.setPrice(new BigDecimal(price));
        t.setPeriodDays(periodDays);
        t.setAvailable(true);
        return tariffs.save(t);
    }

    private static SubscriptionLine line(Tariff tariff, int periods) {
        SubscriptionLine l = new SubscriptionLine();
        l.setTariff(Ref.of(Tariff.class, tariff.getId()));
        l.setPeriods(periods);
        return l;
    }
}