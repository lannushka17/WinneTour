package com.sab.winery.persistence.contract;

import com.sab.winery.persistence.entity.AddonService;
import java.util.List;
import java.util.Optional;

public interface AddonServiceRepository {
    Optional<AddonService> findById(int id);

    List<AddonService> findAll();

    List<AddonService> findAllActive();

    AddonService save(AddonService service);

    boolean deleteById(int id);
}
