package com.sab.winery.persistence.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class WineProduct {
    int id;
    int wineryId;
    String name;
    String category;
    double price;
    int stockUnits;
    String imagePath;

    @Builder.Default
    boolean active = true;
}
