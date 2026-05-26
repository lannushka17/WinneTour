package com.sab.winery.persistence.entity;

/** Категорія додаткової послуги під час відвідування виноробні. */
public enum AddonServiceKind {
    TASTING_EXTRA("Розширена дегустація"),
    SNACK_PLATTER("Закуски / сирна тарілка"),
    SOUVENIR_BOTTLE("Сувенірна пляшка"),
    TRANSFER("Трансфер"),
    MASTERCLASS("Майстер-клас");

    private final String displayName;

    AddonServiceKind(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AddonServiceKind fromString(String value) {
        if (value == null) {
            return TASTING_EXTRA;
        }
        for (AddonServiceKind k : values()) {
            if (k.name().equalsIgnoreCase(value) || k.displayName.equalsIgnoreCase(value)) {
                return k;
            }
        }
        return TASTING_EXTRA;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
