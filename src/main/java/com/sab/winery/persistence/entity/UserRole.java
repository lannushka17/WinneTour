package com.sab.winery.persistence.entity;

/** Ролі користувачів у системі WineryTours. */
public enum UserRole {
    ADMIN("Адміністратор"),
    MANAGER("Менеджер залу"),
    SOMMELIER("Сомельє");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
