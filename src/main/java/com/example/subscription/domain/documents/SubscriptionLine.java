package com.example.subscription.domain.documents;

import com.example.subscription.domain.catalogs.Tariff;
import su.onno.annotations.Attribute;
import su.onno.model.TabularSectionRow;
import su.onno.types.Ref;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One line of a {@link Subscription}: a tariff purchased for N periods.
 *
 * <p>{@code price} and {@code amount} are <b>derived</b> fields — the user never edits them
 * directly. {@code Subscription.beforeWrite()} re-reads the tariff's current price on every
 * save and recomputes {@code amount = price × periods}. Once the subscription has posted,
 * the resulting balance and revenue movements are frozen, so a later change to the tariff's
 * price cannot retroactively alter the register — but until posting, the line's commercial
 * terms follow the tariff.</p>
 */
@Getter
@Setter
public class SubscriptionLine extends TabularSectionRow {

    @Attribute(displayName = "Tariff")
    private Ref<Tariff> tariff;

    @Attribute(displayName = "Periods", precision = 10, scale = 0)
    private Integer periods = 1;

    @Attribute(displayName = "Price", precision = 15, scale = 2)
    private BigDecimal price;

    @Attribute(displayName = "Amount", precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;
}