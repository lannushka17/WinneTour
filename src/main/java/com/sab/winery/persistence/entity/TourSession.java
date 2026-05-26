package com.sab.winery.persistence.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class TourSession {
    int id;
    int programId;
    /** ISO-8601 локальний рядок */
    String startTime;
    int capacity;
    String status;
}
