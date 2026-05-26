package com.sab.winery.persistence.dto;

import lombok.Builder;
import lombok.Value;

/** Варіант сесії в комбобоксі бронювання (підпис з туром і вільними місцями). */
@Value
@Builder
public class SessionPickOption {
    int sessionId;
    String programTitle;
    String startTime;
    int capacity;
    int freePlaces;

    @Override
    public String toString() {
        return "#"
                + sessionId
                + " · "
                + programTitle
                + " · "
                + startTime
                + " · вільно "
                + freePlaces
                + "/"
                + capacity;
    }
}
