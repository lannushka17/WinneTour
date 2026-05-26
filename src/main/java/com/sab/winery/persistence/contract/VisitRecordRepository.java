package com.sab.winery.persistence.contract;

import com.sab.winery.persistence.entity.VisitRecord;
import java.util.Optional;

public interface VisitRecordRepository {
    Optional<VisitRecord> findByReservationId(int reservationId);

    VisitRecord save(VisitRecord v);
}
