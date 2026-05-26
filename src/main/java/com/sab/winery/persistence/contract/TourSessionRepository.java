package com.sab.winery.persistence.contract;

import com.sab.winery.persistence.entity.TourSession;
import java.util.List;
import java.util.Optional;

public interface TourSessionRepository {
    List<TourSession> findByProgramId(int programId);

    Optional<TourSession> findById(int id);

    TourSession save(TourSession s);

    int sumBookedSeatsExcludingReservation(int sessionId, int excludeReservationId);

    int sumBookedSeats(int sessionId);
}
