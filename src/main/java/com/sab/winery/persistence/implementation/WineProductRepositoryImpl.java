package com.sab.winery.persistence.implementation;

import com.sab.winery.persistence.contract.WineProductRepository;
import com.sab.winery.persistence.entity.WineProduct;
import com.sab.winery.persistence.utility.PersistenceConfig;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WineProductRepositoryImpl implements WineProductRepository {

    @Override
    public List<WineProduct> findAll() {
        List<WineProduct> list = new ArrayList<>();
        String sql =
                "SELECT id, winery_id, name, category, price, stock_units, image_path, active FROM wine_products ORDER BY name";
        try (var con = PersistenceConfig.getConnection();
                var st = con.createStatement();
                var rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public Optional<WineProduct> findById(int id) {
        String sql =
                "SELECT id, winery_id, name, category, price, stock_units, image_path, active FROM wine_products WHERE id=?";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            var rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(map(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public WineProduct save(WineProduct w) {
        if (w.getId() == 0) {
            return insert(w);
        }
        return update(w);
    }

    private WineProduct insert(WineProduct w) {
        String sql = "INSERT INTO wine_products(winery_id, name, category, price, stock_units, image_path, active) "
                + "VALUES(?,?,?,?,?,?,?)";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, w.getWineryId());
            ps.setString(2, w.getName());
            ps.setString(3, w.getCategory());
            ps.setDouble(4, w.getPrice());
            ps.setInt(5, w.getStockUnits());
            ps.setString(6, w.getImagePath());
            ps.setInt(7, w.isActive() ? 1 : 0);
            ps.executeUpdate();
            var gk = ps.getGeneratedKeys();
            if (gk.next()) {
                return w.toBuilder().id(gk.getInt(1)).build();
            }
            throw new RuntimeException("No generated id");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private WineProduct update(WineProduct w) {
        String sql =
                "UPDATE wine_products SET winery_id=?, name=?, category=?, price=?, stock_units=?, image_path=?, active=? "
                        + "WHERE id=?";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, w.getWineryId());
            ps.setString(2, w.getName());
            ps.setString(3, w.getCategory());
            ps.setDouble(4, w.getPrice());
            ps.setInt(5, w.getStockUnits());
            ps.setString(6, w.getImagePath());
            ps.setInt(7, w.isActive() ? 1 : 0);
            ps.setInt(8, w.getId());
            ps.executeUpdate();
            return w;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private WineProduct map(ResultSet rs) throws SQLException {
        return WineProduct.builder()
                .id(rs.getInt("id"))
                .wineryId(rs.getInt("winery_id"))
                .name(rs.getString("name"))
                .category(rs.getString("category"))
                .price(rs.getDouble("price"))
                .stockUnits(rs.getInt("stock_units"))
                .imagePath(rs.getString("image_path"))
                .active(rs.getInt("active") == 1)
                .build();
    }
}
