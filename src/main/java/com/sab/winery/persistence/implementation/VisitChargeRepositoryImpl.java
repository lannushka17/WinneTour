package com.sab.winery.persistence.implementation;

import com.sab.winery.persistence.contract.VisitChargeRepository;
import com.sab.winery.persistence.entity.VisitCharge;
import com.sab.winery.persistence.utility.PersistenceConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VisitChargeRepositoryImpl implements VisitChargeRepository {

    private static final String SQL_FIND_BY_ID = "SELECT * FROM visit_charges WHERE id=?";
    private static final String SQL_FIND_ALL = "SELECT * FROM visit_charges ORDER BY usage_date_time DESC";
    private static final String SQL_FIND_BY_RESERVATION =
            "SELECT * FROM visit_charges WHERE reservation_id=? ORDER BY usage_date_time DESC";
    private static final String SQL_INSERT =
            "INSERT INTO visit_charges(reservation_id, addon_service_id, quantity, usage_date_time, paid) VALUES (?,?,?,?,?)";
    private static final String SQL_UPDATE =
            "UPDATE visit_charges SET reservation_id=?, addon_service_id=?, quantity=?, usage_date_time=?, paid=? WHERE id=?";
    private static final String SQL_DELETE = "DELETE FROM visit_charges WHERE id=?";

    @Override
    public Optional<VisitCharge> findById(int id) {
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
    public List<VisitCharge> findAll() {
        List<VisitCharge> list = new ArrayList<>();
        try (Connection con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_FIND_ALL);
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
    public List<VisitCharge> findByReservationId(int reservationId) {
        List<VisitCharge> list = new ArrayList<>();
        try (Connection con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_RESERVATION)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public VisitCharge save(VisitCharge c) {
        return c.getId() == 0 ? insert(c) : update(c);
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

    private VisitCharge insert(VisitCharge c) {
        try (Connection con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getReservationId());
            ps.setInt(2, c.getAddonServiceId());
            ps.setInt(3, c.getQuantity());
            ps.setString(4, c.getUsageDateTime().toString());
            ps.setInt(5, c.isPaid() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return c.toBuilder().id(keys.getInt(1)).build();
                }
                throw new RuntimeException("Не отримано згенерований ID нарахування");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private VisitCharge update(VisitCharge c) {
        try (Connection con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {
            ps.setInt(1, c.getReservationId());
            ps.setInt(2, c.getAddonServiceId());
            ps.setInt(3, c.getQuantity());
            ps.setString(4, c.getUsageDateTime().toString());
            ps.setInt(5, c.isPaid() ? 1 : 0);
            ps.setInt(6, c.getId());
            ps.executeUpdate();
            return c;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private VisitCharge map(ResultSet rs) throws SQLException {
        return VisitCharge.builder()
                .id(rs.getInt("id"))
                .reservationId(rs.getInt("reservation_id"))
                .addonServiceId(rs.getInt("addon_service_id"))
                .quantity(rs.getInt("quantity"))
                .usageDateTime(LocalDateTime.parse(rs.getString("usage_date_time")))
                .paid(rs.getInt("paid") == 1)
                .build();
    }
}
