package com.example.subscription;

import com.example.subscription.domain.catalogs.Customer;
import com.example.subscription.domain.catalogs.Tariff;
import com.example.subscription.domain.documents.Subscription;
import com.example.subscription.domain.documents.SubscriptionLine;
import com.example.subscription.domain.enumerations.SubscriptionStatus;
import com.example.subscription.repositories.TariffRepository;
import su.onno.rules.BusinessRule;
import su.onno.types.Ref;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the {@link Subscription#rules()} contract in isolation, without going through the
 * write pipeline. Each rule is identified by its name so the test fails with a precise message
 * if a rule is renamed or removed.
 */
class SubscriptionRulesTest extends AbstractIntegrationTest {

    @Autowired TariffRepository tariffs;

    @Test
    void customerRequired_violatedWhenNull() {
        Subscription sub = new Subscription();
        sub.setStatus(SubscriptionStatus.DRAFT);

        assertThat(ruleHolds(sub, "customer-required")).isFalse();
    }

    @Test
    void customerRequired_holdsWhenSet() {
        Subscription sub = new Subscription();
        sub.setCustomer(Ref.of(Customer.class, UUID.randomUUID()));
        sub.setStatus(SubscriptionStatus.DRAFT);

        assertThat(ruleHolds(sub, "customer-required")).isTrue();
    }

    @Test
    void linesRequired_violatedWhenEmptyDraft() {
        Subscription sub = new Subscription();
        sub.setCustomer(Ref.of(Customer.class, UUID.randomUUID()));
        sub.setStatus(SubscriptionStatus.DRAFT);

        assertThat(ruleHolds(sub, "lines-required")).isFalse();
    }

    @Test
    void linesRequired_allowedWhenCancelled() {
        Subscription sub = new Subscription();
        sub.setCustomer(Ref.of(Customer.class, UUID.randomUUID()));
        sub.setStatus(SubscriptionStatus.CANCELLED);

        // A cancelled subscription is a terminal record with a reason only — no lines required.
        assertThat(ruleHolds(sub, "lines-required")).isTrue();
    }

    @Test
    void periodsPositive_violatedWhenZero() {
        Subscription sub = subscriptionWithLine(periods(0), availableTariff());

        assertThat(ruleHolds(sub, "periods-positive")).isFalse();
    }

    @Test
    void periodsPositive_violatedWhenNull() {
        Subscription sub = subscriptionWithLine(periods(null), availableTariff());

        assertThat(ruleHolds(sub, "periods-positive")).isFalse();
    }

    @Test
    void periodsPositive_holdsWhenPositive() {
        Subscription sub = subscriptionWithLine(periods(3), availableTariff());

        assertThat(ruleHolds(sub, "periods-positive")).isTrue();
    }

    @Test
    void tariffAvailable_violatedWhenTariffDisabled() {
        Subscription sub = subscriptionWithLine(periods(1), unavailableTariff());

        assertThat(ruleHolds(sub, "tariff-available")).isFalse();
    }

    @Test
    void tariffAvailable_holdsWhenAvailable() {
        Subscription sub = subscriptionWithLine(periods(1), availableTariff());

        assertThat(ruleHolds(sub, "tariff-available")).isTrue();
    }

    // --- helpers -------------------------------------------------------------------------------

    private boolean ruleHolds(Subscription sub, String ruleName) {
        return sub.rules().stream()
                .filter(r -> ruleName.equals(r.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No rule named '" + ruleName + "'"))
                .holds();
    }

    private Subscription subscriptionWithLine(Integer periods, Tariff tariff) {
        Subscription sub = new Subscription();
        sub.setCustomer(Ref.of(Customer.class, UUID.randomUUID()));
        sub.setStatus(SubscriptionStatus.DRAFT);
        SubscriptionLine line = new SubscriptionLine();
        line.setTariff(Ref.of(Tariff.class, tariff.getId()));
        line.setPeriods(periods);
        sub.getLines().add(line);
        return sub;
    }

    private Integer periods(Integer value) {
        return value;
    }

    private Tariff availableTariff() {
        return newTariff(true);
    }

    private Tariff unavailableTariff() {
        return newTariff(false);
    }

    private Tariff newTariff(boolean available) {
        Tariff t = new Tariff();
        t.setDescription("Tariff-" + UUID.randomUUID().toString().substring(0, 8));
        t.setPrice(new BigDecimal("30.00"));
        t.setPeriodDays(30);
        t.setAvailable(available);
        return tariffs.save(t);
    }
}