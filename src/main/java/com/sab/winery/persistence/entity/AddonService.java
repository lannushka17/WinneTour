package com.sab.winery.persistence.entity;

import lombok.Builder;
import lombok.Data;

/** Каталог додаткових послуг, які можна нарахувати поверх бронювання туру. */
@Data
@Builder(toBuilder = true)
public class AddonService {
    int id;
    String name;
    AddonServiceKind kind;
    double pricePerUnit;

    @Builder.Default
    boolean active = true;

    @Override
    public String toString() {
        return String.format("%s (%.2f грн)", name, pricePerUnit);
    }
}
