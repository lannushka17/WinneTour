package com.sab.winery.persistence.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class VisitRecord {
    int id;
    int reservationId;
    int attendedCount;
    String notes;
    /** ATTENDED або NO_SHOW */
    String status;
}
