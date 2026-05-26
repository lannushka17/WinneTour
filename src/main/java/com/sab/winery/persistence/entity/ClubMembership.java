package com.sab.winery.persistence.entity;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

/**
 * Членство гостя у винному клубі виноробні. Гість отримує постійний знижковий
 * рівень на сесії турів та додаткові послуги, поки картка активна.
 */
@Data
@Builder(toBuilder = true)
public class ClubMembership {
    int id;
    int guestId;
    ClubTier tier;
    LocalDate validUntil;
    int discountPercent;

    @Builder.Default
    boolean active = true;
}
