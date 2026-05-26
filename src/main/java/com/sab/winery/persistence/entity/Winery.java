package com.sab.winery.persistence.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class Winery {
    int id;
    String name;

    @Override
    public String toString() {
        return name;
    }
}
