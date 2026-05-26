package com.sab.winery.persistence.contract;

import com.sab.winery.persistence.entity.WineProduct;
import java.util.List;
import java.util.Optional;

public interface WineProductRepository {
    List<WineProduct> findAll();

    Optional<WineProduct> findById(int id);

    WineProduct save(WineProduct w);
}
