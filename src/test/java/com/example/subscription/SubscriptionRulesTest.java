package com.example.subscription;

import com.example.subscription.domain.catalogs.Customer;
import com.example.subscription.domain.catalogs.Tariff;
import com.example.subscription.domain.documents.Subscription;
import com.example.subscription.domain.documents.SubscriptionLine;
import com.example.subscription.domain.enumerations.SubscriptionStatus;
import com.example.subscription.repositories.TariffRepository;
import su.onno.types.Ref;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the {@link Subscription#rules()} contract in isolation, without going through the
 * write pipeline. Each rule is identified by its name so the test fails with a precise message
 * if a rule is renamed or removed.
 *
 * <p>Traces to Level 2 business-rule requirements: a customer is required, at least one line
 * is required, every line must have a positive period count, and every selected tariff must be
 * available for subscription.</p>
 */
@Tag("level-2")
class SubscriptionRulesTest extends AbstractIntegrationTest {

    @Autowired TariffRepository tariffs;

    @Nested
    @DisplayName("customer-required")
    class CustomerRequired {

        @Test
        @DisplayName("[Level 2 · rules] A subscription without a customer violates the rule")
        void violatedWhenNull() {
            Subscription sub = new Subscription();
            sub.setStatus(SubscriptionStatus.DRAFT);

            assertThat(ruleHolds(sub, "customer-required")).isFalse();
        }

        @Test
        @DisplayName("[Level 2 · rules] A subscription with a customer passes")
        void holdsWhenSet() {
            Subscription sub = new Subscription();
            sub.setCustomer(Ref.of(Customer.class, UUID.randomUUID()));
            sub.setStatus(SubscriptionStatus.DRAFT);

            assertThat(ruleHolds(sub, "customer-required")).isTrue();
        }
    }

    @Nested
    @DisplayName("lines-required")
    class LinesRequired {

        @Test
        @DisplayName("[Level 2 · rules] An empty draft violates the rule")
        void violatedWhenEmptyDraft() {
            Subscription sub = new Subscription();
            sub.setCustomer(Ref.of(Customer.class, UUID.randomUUID()));
            sub.setStatus(SubscriptionStatus.DRAFT);

            assertThat(ruleHolds(sub, "lines-required")).isFalse();
        }

        @Test
        @DisplayName("[Level 2 · rules] A cancelled subscription is allowed to carry no lines")
        void allowedWhenCancelled() {
            Subscription sub = new Subscription();
            sub.setCustomer(Ref.of(Customer.class, UUID.randomUUID()));
            sub.setStatus(SubscriptionStatus.CANCELLED);

            assertThat(ruleHolds(sub, "lines-required")).isTrue();
        }
    }

    @Nested
    @DisplayName("periods-positive")
    class PeriodsPositive {

        @Test
        @DisplayName("[Level 2 · rules] Zero periods violates the rule")
        void violatedWhenZero() {
            Subscription sub = subscriptionWithLine(0, availableTariff());
            assertThat(ruleHolds(sub, "periods-positive")).isFalse();
        }

        @Test
        @DisplayName("[Level 2 · rules] Null periods violates the rule")
        void violatedWhenNull() {
            Subscription sub = subscriptionWithLine(null, availableTariff());
            assertThat(ruleHolds(sub, "periods-positive")).isFalse();
        }

        @Test
        @DisplayName("[Level 2 · rules] Positive periods pass")
        void holdsWhenPositive() {
            Subscription sub = subscriptionWithLine(3, availableTariff());
            assertThat(ruleHolds(sub, "periods-positive")).isTrue();
        }
    }

    @Nested
    @DisplayName("tariff-available")
    class TariffAvailable {

        @Test
        @DisplayName("[Level 2 · rules] A retired tariff violates the rule")
        void violatedWhenTariffDisabled() {
            Subscription sub = subscriptionWithLine(1, unavailableTariff());
            assertThat(ruleHolds(sub, "tariff-available")).isFalse();
        }

        @Test
        @DisplayName("[Level 2 · rules] An available tariff passes")
        void holdsWhenAvailable() {
            Subscription sub = subscriptionWithLine(1, availableTariff());
            assertThat(ruleHolds(sub, "tariff-available")).isTrue();
        }
    }

    // --- helpers -------------------------------------------------------------------------------

    private boolean ruleHolds(Subscription sub, String ruleName) {
        return sub.rules().stream()
                .filter(r -> ruleName.equals(r.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No rule named '" + ruleName + "'"))
                .holds();
    }

    private Subscription subscriptionWithLine(Integer periods, Ref<Tariff> tariff) {
        Subscription sub = new Subscription();
        sub.setCustomer(Ref.of(Customer.class, UUID.randomUUID()));
        sub.setStatus(SubscriptionStatus.DRAFT);
        SubscriptionLine line = new SubscriptionLine();
        line.setTariff(tariff);
        line.setPeriods(periods);
        sub.getLines().add(line);
        return sub;
    }

    private Ref<Tariff> availableTariff() {
        return newTariff(true);
    }

    private Ref<Tariff> unavailableTariff() {
        return newTariff(false);
    }

    @SuppressWarnings("SameParameterValue")
    private Ref<Tariff> newTariff(boolean available) {
        Tariff t = new Tariff();
        t.setDescription("Tariff-" + UUID.randomUUID().toString().substring(0, 8));
        t.setPrice(new BigDecimal("30.00"));
        t.setPeriodDays(30);
        t.setAvailable(available);
        return Ref.of(Tariff.class, Objects.requireNonNull(tariffs.save(t).getId()));
    }
}