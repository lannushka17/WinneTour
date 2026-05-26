package com.sab.winery.service;

import com.sab.winery.persistence.contract.ReservationRepository;
import com.sab.winery.persistence.contract.VisitRecordRepository;
import com.sab.winery.persistence.entity.Reservation;
import com.sab.winery.persistence.entity.VisitRecord;
import com.sab.winery.persistence.implementation.ReservationRepositoryImpl;
import com.sab.winery.persistence.implementation.VisitRecordRepositoryImpl;

public class VisitService {

    private final ReservationRepository reservations = new ReservationRepositoryImpl();
    private final VisitRecordRepository visits = new VisitRecordRepositoryImpl();

    /** Фіксує візит для підтвердженого бронювання; скасовані бронювання не приймаються. */
    public VisitRecord recordAttendance(int reservationId, int attendedCount, String notes, boolean noShow) {
        Reservation r =
                reservations.findById(reservationId).orElseThrow(() -> new IllegalArgumentException("Бронювання не знайдено"));
        if (!"CONFIRMED".equalsIgnoreCase(r.getStatus())) {
            throw new IllegalStateException("Можна фіксувати візит лише для підтвердженого бронювання");
        }
        if (!noShow && (attendedCount < 0 || attendedCount > r.getPartySize())) {
            throw new IllegalArgumentException("Кількість присутніх має бути від 0 до " + r.getPartySize());
        }

        String status = noShow ? "NO_SHOW" : "ATTENDED";
        int effectiveAttended = noShow ? 0 : attendedCount;

        return visits.findByReservationId(reservationId)
                .map(
                        existing ->
                                visits.save(
                                        existing.toBuilder()
                                                .attendedCount(effectiveAttended)
                                                .notes(notes != null ? notes : "")
                                                .status(status)
                                                .build()))
                .orElseGet(
                        () ->
                                visits.save(
                                        VisitRecord.builder()
                                                .reservationId(reservationId)
                                                .attendedCount(effectiveAttended)
                                                .notes(notes != null ? notes : "")
                                                .status(status)
                                                .build()));
    }

    public void cancelReservation(int reservationId) {
        Reservation r =
                reservations.findById(reservationId).orElseThrow(() -> new IllegalArgumentException("Бронювання не знайдено"));
        if ("CANCELLED".equalsIgnoreCase(r.getStatus())) {
            return;
        }
        reservations.updateStatus(reservationId, "CANCELLED");
    }
}
