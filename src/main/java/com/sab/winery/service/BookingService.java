package com.sab.winery.service;

import com.sab.winery.persistence.contract.GuestProfileRepository;
import com.sab.winery.persistence.contract.ReservationRepository;
import com.sab.winery.persistence.contract.TourSessionRepository;
import com.sab.winery.persistence.entity.Reservation;
import com.sab.winery.persistence.entity.TourSession;
import com.sab.winery.persistence.implementation.GuestProfileRepositoryImpl;
import com.sab.winery.persistence.implementation.ReservationRepositoryImpl;
import com.sab.winery.persistence.implementation.TourSessionRepositoryImpl;
import java.time.LocalDateTime;
import java.util.Optional;

public class BookingService {

    private final TourSessionRepository sessions = new TourSessionRepositoryImpl();
    private final GuestProfileRepository guests = new GuestProfileRepositoryImpl();
    private final ReservationRepository reservations = new ReservationRepositoryImpl();

    public Reservation bookForGuest(int sessionId, int guestId, int partySize) {
        if (partySize < 1) {
            throw new IllegalArgumentException("Кількість гостей має бути не менше 1");
        }
        TourSession session =
                sessions.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("Сесію не знайдено"));
        if (!"SCHEDULED".equalsIgnoreCase(session.getStatus())) {
            throw new IllegalStateException("Сесію не можна забронювати");
        }
        guests.findById(guestId).orElseThrow(() -> new IllegalArgumentException("Гостя не знайдено"));

        int booked = sessions.sumBookedSeats(sessionId);
        if (booked + partySize > session.getCapacity()) {
            throw new IllegalStateException(
                    "Перевищено місткість сесії. Вільно місць: " + (session.getCapacity() - booked));
        }

        Reservation r =
                Reservation.builder()
                        .sessionId(sessionId)
                        .guestId(guestId)
                        .partySize(partySize)
                        .status("CONFIRMED")
                        .createdAt(LocalDateTime.now().toString())
                        .build();
        return reservations.save(r);
    }

    public Optional<TourSession> findSession(int id) {
        return sessions.findById(id);
    }

    /** Вільних місць на сесію з урахуванням підтверджених бронювань. */
    public int freePlaces(int sessionId) {
        TourSession session =
                sessions.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("Сесію не знайдено"));
        int booked = sessions.sumBookedSeats(sessionId);
        return Math.max(0, session.getCapacity() - booked);
    }
}
