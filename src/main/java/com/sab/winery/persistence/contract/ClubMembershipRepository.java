package com.sab.winery.persistence.contract;

import com.sab.winery.persistence.entity.ClubMembership;
import java.util.List;
import java.util.Optional;

public interface ClubMembershipRepository {
    Optional<ClubMembership> findById(int id);

    Optional<ClubMembership> findActiveByGuestId(int guestId);

    List<ClubMembership> findAll();

    ClubMembership save(ClubMembership membership);

    boolean deleteById(int id);
}
