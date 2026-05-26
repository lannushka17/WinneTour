package com.sab.winery.persistence.dto;

import lombok.Builder;
import lombok.Data;

/** Рядок списку бронювань (результат JOIN-запиту). */
@Data
@Builder
public class ReservationListRow {
    private int reservationId;
    private String guestName;
    private String guestEmail;
    private String programTitle;
    private String sessionStart;
    private int partySize;
    private String reservationStatus;
    /** Короткий текст про візит або «—». */
    private String visitInfo;
}
