package com.sab.winery.persistence.contract;

import com.sab.winery.persistence.entity.Winery;
import java.util.List;

public interface WineryRepository {
    List<Winery> findAll();
}
