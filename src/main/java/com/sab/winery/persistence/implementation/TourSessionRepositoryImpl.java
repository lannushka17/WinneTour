package com.sab.winery.persistence.implementation;

import com.sab.winery.persistence.contract.TourSessionRepository;
import com.sab.winery.persistence.entity.TourSession;
import com.sab.winery.persistence.utility.PersistenceConfig;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TourSessionRepositoryImpl implements TourSessionRepository {

    @Override
    public List<TourSession> findByProgramId(int programId) {
        List<TourSession> list = new ArrayList<>();
        String sql = "SELECT id, program_id, start_time, capacity, status FROM tour_sessions WHERE program_id=? ORDER BY start_time";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, programId);
            var rs = ps.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public Optional<TourSession> findById(int id) {
        String sql = "SELECT id, program_id, start_time, capacity, status FROM tour_sessions WHERE id=?";
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
    public TourSession save(TourSession s) {
        if (s.getId() == 0) {
            return insert(s);
        }
        return update(s);
    }

    @Override
    public int sumBookedSeatsExcludingReservation(int sessionId, int excludeReservationId) {
        String sql =
                "SELECT COALESCE(SUM(party_size),0) FROM reservations WHERE session_id=? AND status='CONFIRMED' AND id<>?";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, excludeReservationId);
            var rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int sumBookedSeats(int sessionId) {
        String sql = "SELECT COALESCE(SUM(party_size),0) FROM reservations WHERE session_id=? AND status='CONFIRMED'";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            var rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private TourSession insert(TourSession s) {
        String sql = "INSERT INTO tour_sessions(program_id, start_time, capacity, status) VALUES(?,?,?,?)";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getProgramId());
            ps.setString(2, s.getStartTime());
            ps.setInt(3, s.getCapacity());
            ps.setString(4, s.getStatus());
            ps.executeUpdate();
            var gk = ps.getGeneratedKeys();
            if (gk.next()) {
                return s.toBuilder().id(gk.getInt(1)).build();
            }
            throw new RuntimeException("No generated id");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private TourSession update(TourSession s) {
        String sql = "UPDATE tour_sessions SET program_id=?, start_time=?, capacity=?, status=? WHERE id=?";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, s.getProgramId());
            ps.setString(2, s.getStartTime());
            ps.setInt(3, s.getCapacity());
            ps.setString(4, s.getStatus());
            ps.setInt(5, s.getId());
            ps.executeUpdate();
            return s;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private TourSession map(ResultSet rs) throws SQLException {
        return TourSession.builder()
                .id(rs.getInt("id"))
                .programId(rs.getInt("program_id"))
                .startTime(rs.getString("start_time"))
                .capacity(rs.getInt("capacity"))
                .status(rs.getString("status"))
                .build();
    }
}
