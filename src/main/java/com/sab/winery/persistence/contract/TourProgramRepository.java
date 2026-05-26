package com.sab.winery.persistence.contract;

import com.sab.winery.persistence.entity.TourProgram;
import java.util.List;
import java.util.Optional;

public interface TourProgramRepository {
    List<TourProgram> findAll();

    List<TourProgram> findAllActive();

    Optional<TourProgram> findById(int id);

    TourProgram save(TourProgram p);

    void setActive(int id, boolean active);
}
