package com.sab.winery.persistence.contract;

import com.sab.winery.persistence.entity.GuestProfile;
import java.util.List;
import java.util.Optional;

public interface GuestProfileRepository {
    Optional<GuestProfile> findByEmailIgnoreCase(String email);

    Optional<GuestProfile> findById(int id);

    List<GuestProfile> findAllOrderByName();

    GuestProfile save(GuestProfile g);

    void deleteById(int id);

    int countActiveReservationsForGuest(int guestId);
}
