package com.sab.winery.persistence.entity;

import lombok.Builder;
import lombok.Data;

/** Користувач інформаційної системи WineryTours. */
@Data
@Builder(toBuilder = true)
public class AppUser {
    int id;
    String username;
    String passwordHash;
    UserRole role;
    String fullName;
}
