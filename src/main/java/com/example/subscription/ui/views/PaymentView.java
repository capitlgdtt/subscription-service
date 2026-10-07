package com.example.subscription.ui.views;

import com.example.subscription.domain.documents.Payment;
import su.onno.ui.EntityConfigBuilder;
import su.onno.ui.EntityView;
import su.onno.ui.ListSpec;

import org.springframework.stereotype.Component;

@Component
public class PaymentView implements EntityView<Payment> {

    @Override
    public Class<Payment> entity() {
        return Payment.class;
    }

    @Override
    public void list(ListSpec<Payment> list) {
        list.columns(Payment::getNumber, Payment::getDate, Payment::getCustomer,
                        Payment::getAmount, Payment::getMethod, Payment::isPosted)
                .label(Payment::getAmount, "Amount")
                .label(Payment::getMethod, "Method")
                .label(Payment::isPosted, "Posted")
                .sortBy(Payment::getDate, true);
    }

    @Override
    public void fields(EntityConfigBuilder<Payment> f) {
        f.field(Payment::getCustomer).order(0).width("half")
                .field(Payment::getMethod).order(1).width("half")
                .field(Payment::getDate).order(2).width("half").format("dd-MM-yyyy HH:mm")
                .field(Payment::getAmount).order(3).width("half").format("currency:USD")
                .field(Payment::getNote).order(4).widget("textarea");
    }
}