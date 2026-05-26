package com.sab.winery.persistence.implementation;

import com.sab.winery.persistence.contract.VisitRecordRepository;
import com.sab.winery.persistence.entity.VisitRecord;
import com.sab.winery.persistence.utility.PersistenceConfig;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class VisitRecordRepositoryImpl implements VisitRecordRepository {

    @Override
    public Optional<VisitRecord> findByReservationId(int reservationId) {
        String sql = "SELECT id, reservation_id, attended_count, notes, status FROM visit_records WHERE reservation_id=?";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
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
    public VisitRecord save(VisitRecord v) {
        if (v.getId() == 0) {
            return insert(v);
        }
        return update(v);
    }

    private VisitRecord insert(VisitRecord v) {
        String sql = "INSERT INTO visit_records(reservation_id, attended_count, notes, status) VALUES(?,?,?,?)";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, v.getReservationId());
            ps.setInt(2, v.getAttendedCount());
            ps.setString(3, v.getNotes());
            ps.setString(4, v.getStatus());
            ps.executeUpdate();
            var gk = ps.getGeneratedKeys();
            if (gk.next()) {
                return v.toBuilder().id(gk.getInt(1)).build();
            }
            throw new RuntimeException("No generated id");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private VisitRecord update(VisitRecord v) {
        String sql = "UPDATE visit_records SET attended_count=?, notes=?, status=? WHERE id=?";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, v.getAttendedCount());
            ps.setString(2, v.getNotes());
            ps.setString(3, v.getStatus());
            ps.setInt(4, v.getId());
            ps.executeUpdate();
            return v;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private VisitRecord map(ResultSet rs) throws SQLException {
        return VisitRecord.builder()
                .id(rs.getInt("id"))
                .reservationId(rs.getInt("reservation_id"))
                .attendedCount(rs.getInt("attended_count"))
                .notes(rs.getString("notes"))
                .status(rs.getString("status"))
                .build();
    }
}
