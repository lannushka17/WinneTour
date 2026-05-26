package com.sab.winery.persistence.contract;

import com.sab.winery.persistence.entity.VisitCharge;
import java.util.List;
import java.util.Optional;

public interface VisitChargeRepository {
    Optional<VisitCharge> findById(int id);

    List<VisitCharge> findAll();

    List<VisitCharge> findByReservationId(int reservationId);

    VisitCharge save(VisitCharge charge);

    boolean deleteById(int id);
}
