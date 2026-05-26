package com.sab.winery.persistence.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class GuestProfile {
    int id;
    String name;
    String email;
    String phone;
}
