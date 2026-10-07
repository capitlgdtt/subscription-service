package com.example.subscription.ui.views;

import com.example.subscription.domain.catalogs.Tariff;
import su.onno.ui.EntityConfigBuilder;
import su.onno.ui.EntityView;
import su.onno.ui.ListSpec;

import org.springframework.stereotype.Component;

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