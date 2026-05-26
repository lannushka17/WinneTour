package com.sab.winery.persistence.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class TourProgram {
    int id;
    int wineryId;
    String title;
    String description;
    int durationMinutes;
    int defaultMaxGuests;
    String imagePath;

    @Builder.Default
    boolean active = true;

    @Override
    public String toString() {
        return title;
    }
}
