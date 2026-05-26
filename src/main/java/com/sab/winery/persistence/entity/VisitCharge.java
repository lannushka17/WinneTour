package com.sab.winery.persistence.entity;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/** Нарахування за додаткову послугу, прив'язане до бронювання гостя. */
@Data
@Builder(toBuilder = true)
public class VisitCharge {
    int id;
    int reservationId;
    int addonServiceId;
    int quantity;
    LocalDateTime usageDateTime;

    @Builder.Default
    boolean paid = false;

    public double getTotalAmount(double pricePerUnit) {
        return quantity * pricePerUnit;
    }
}
