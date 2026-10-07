package com.example.subscription.domain.documents;

import com.example.subscription.domain.catalogs.Tariff;
import su.onno.annotations.Attribute;
import su.onno.model.TabularSectionRow;
import su.onno.types.Ref;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One line of a {@link Subscription}: a tariff purchased for N periods at a frozen price.
 *
 * <p>The {@code price} is captured once, when the tariff is first picked, and is never
 * re-read from the tariff afterward. This freezes the commercial terms of a posted
 * subscription: editing a tariff's price later must not retroactively change the total
 * of an existing document (and therefore must not diverge from the revenue register).
 * The {@code amount} is derived as {@code price * periods} on every write; {@code periods}
 * is the only field the user can freely change on an existing line.</p>
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