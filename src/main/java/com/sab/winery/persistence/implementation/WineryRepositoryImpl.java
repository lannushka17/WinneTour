package com.sab.winery.persistence.implementation;

import com.sab.winery.persistence.contract.WineryRepository;
import com.sab.winery.persistence.entity.Winery;
import com.sab.winery.persistence.utility.PersistenceConfig;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class WineryRepositoryImpl implements WineryRepository {
    @Override
    public List<Winery> findAll() {
        List<Winery> list = new ArrayList<>();
        try (var con = PersistenceConfig.getConnection();
                Statement st = con.createStatement();
                var rs = st.executeQuery("SELECT id, name FROM wineries ORDER BY id")) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    private Winery map(ResultSet rs) throws SQLException {
        return Winery.builder().id(rs.getInt("id")).name(rs.getString("name")).build();
    }
}
