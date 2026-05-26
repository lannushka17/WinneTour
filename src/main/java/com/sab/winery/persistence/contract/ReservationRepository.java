package com.sab.winery.persistence.contract;

import com.sab.winery.persistence.dto.ReservationListRow;
import com.sab.winery.persistence.entity.Reservation;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation r);

    Optional<Reservation> findById(int id);

    int countByFilters(String search, String statusFilter);

    List<ReservationListRow> findPage(
            String search, String statusFilter, String sortColumn, boolean ascending, int offset, int limit);

    void updateStatus(int id, String status);
}
