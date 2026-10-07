package com.example.subscription.ui.views;

import com.example.subscription.domain.catalogs.Tariff;
import su.onno.ui.EntityConfigBuilder;
import su.onno.ui.EntityView;
import su.onno.ui.ListSpec;

import org.springframework.stereotype.Component;

/**
 * UI shape of the {@link Tariff} catalog.
 *
 * <p>Price is rendered as money in both the list and the form so a manager never mistakes
 * a bare number for a monthly vs. an annual figure; the period length sits beside it as
 * "Days". The "Available" flag uses a switch control — the intent is a simple on/off
 * toggle, not a checkbox whose blank state is ambiguous.</p>
 */
@Component
public class TariffView implements EntityView<Tariff> {

    @Override
    public Class<Tariff> entity() {
        return Tariff.class;
    }

    @Override
    public void list(ListSpec<Tariff> list) {
        list.columns(Tariff::getCode, Tariff::getDescription, Tariff::getPrice,
                        Tariff::getPeriodDays, Tariff::isAvailable)
                .label(Tariff::getDescription, "Name")
                .label(Tariff::getPrice, "Price")
                .label(Tariff::getPeriodDays, "Days")
                .label(Tariff::isAvailable, "Available")
                .sortBy(Tariff::getDescription, false);
    }

    @Override
    public void fields(EntityConfigBuilder<Tariff> f) {
        f.field(Tariff::getDescription).order(0).label("Name")
                .field(Tariff::getPrice).order(1).format("currency:USD")
                .field(Tariff::getPeriodDays).order(2)
                .field(Tariff::isAvailable).order(3).widget("switch");
    }
}