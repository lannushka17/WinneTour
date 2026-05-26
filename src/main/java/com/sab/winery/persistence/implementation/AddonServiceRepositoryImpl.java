package com.sab.winery.persistence.implementation;

import com.sab.winery.persistence.contract.AddonServiceRepository;
import com.sab.winery.persistence.entity.AddonService;
import com.sab.winery.persistence.entity.AddonServiceKind;
import com.sab.winery.persistence.utility.PersistenceConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AddonServiceRepositoryImpl implements AddonServiceRepository {

    private static final String SQL_FIND_BY_ID = "SELECT * FROM addon_services WHERE id=?";
    private static final String SQL_FIND_ALL = "SELECT * FROM addon_services ORDER BY name";
    private static final String SQL_FIND_ACTIVE = "SELECT * FROM addon_services WHERE active=1 ORDER BY name";
    private static final String SQL_INSERT =
            "INSERT INTO addon_services(name, kind, price_per_unit, active) VALUES (?,?,?,?)";
    private static final String SQL_UPDATE =
            "UPDATE addon_services SET name=?, kind=?, price_per_unit=?, active=? WHERE id=?";
    private static final String SQL_DELETE = "DELETE FROM addon_services WHERE id=?";

    @Override
    public Optional<AddonService> findById(int id) {
        try (Connection con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<AddonService> findAll() {
        return findInternal(SQL_FIND_ALL);
    }

    @Override
    public List<AddonService> findAllActive() {
        return findInternal(SQL_FIND_ACTIVE);
    }

    private List<AddonService> findInternal(String sql) {
        List<AddonService> list = new ArrayList<>();
        try (Connection con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public AddonService save(AddonService service) {
        return service.getId() == 0 ? insert(service) : update(service);
    }

    @Override
    public boolean deleteById(int id) {
        try (Connection con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private AddonService insert(AddonService s) {
        try (Connection con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getKind().name());
            ps.setDouble(3, s.getPricePerUnit());
            ps.setInt(4, s.isActive() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return s.toBuilder().id(keys.getInt(1)).build();
                }
                throw new RuntimeException("Не отримано згенерований ID послуги");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private AddonService update(AddonService s) {
        try (Connection con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getKind().name());
            ps.setDouble(3, s.getPricePerUnit());
            ps.setInt(4, s.isActive() ? 1 : 0);
            ps.setInt(5, s.getId());
            ps.executeUpdate();
            return s;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private AddonService map(ResultSet rs) throws SQLException {
        return AddonService.builder()
                .id(rs.getInt("id"))
                .name(rs.getString("name"))
                .kind(AddonServiceKind.fromString(rs.getString("kind")))
                .pricePerUnit(rs.getDouble("price_per_unit"))
                .active(rs.getInt("active") == 1)
                .build();
    }
}
