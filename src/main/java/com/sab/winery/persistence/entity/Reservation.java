package com.sab.winery.persistence.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class Reservation {
    int id;
    int sessionId;
    int guestId;
    int partySize;
    String status;
    String createdAt;
}
