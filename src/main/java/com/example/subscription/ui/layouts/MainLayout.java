package com.example.subscription.ui.layouts;

import com.example.subscription.domain.catalogs.Customer;
import com.example.subscription.domain.catalogs.Tariff;
import com.example.subscription.domain.documents.Payment;
import com.example.subscription.domain.documents.Subscription;
import com.example.subscription.domain.registers.AccountBalance;
import com.example.subscription.domain.registers.Revenue;
import su.onno.ui.Layout;
import su.onno.ui.LayoutSpec;
import su.onno.ui.NavStyle;

import org.springframework.stereotype.Component;

@Component
public class MainLayout implements Layout {

    @Override
    public void configure(LayoutSpec layout) {
        layout.shell()
                .nav(NavStyle.SIDEBAR)
                .brand("Subscriptions");

        layout.section("Sales")
                .order(0)
                .icon("users")
                .catalog(Customer.class)
                .catalog(Tariff.class)
                .document(Payment.class)
                .document(Subscription.class);

        layout.section("Reports")
                .order(10)
                .icon("chart-column")
                .register(AccountBalance.class)
                .register(Revenue.class);
    }
}