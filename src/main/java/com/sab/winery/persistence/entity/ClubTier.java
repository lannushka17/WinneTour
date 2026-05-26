package com.sab.winery.persistence.entity;

/** Рівень картки виноробного клубу. */
public enum ClubTier {
    BRONZE("Бронзовий", 5),
    SILVER("Срібний", 10),
    GOLD("Золотий", 15),
    PLATINUM("Платиновий", 20);

    private final String displayName;
    private final int defaultDiscountPercent;

    ClubTier(String displayName, int defaultDiscountPercent) {
        this.displayName = displayName;
        this.defaultDiscountPercent = defaultDiscountPercent;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDefaultDiscountPercent() {
        return defaultDiscountPercent;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
