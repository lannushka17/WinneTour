package com.sab.winery.persistence.implementation;

import com.sab.winery.persistence.contract.ReservationRepository;
import com.sab.winery.persistence.dto.ReservationListRow;
import com.sab.winery.persistence.entity.Reservation;
import com.sab.winery.persistence.utility.PersistenceConfig;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepositoryImpl implements ReservationRepository {

    private static final String BASE_FROM =
            "FROM reservations r "
                    + "JOIN guest_profiles g ON r.guest_id = g.id "
                    + "JOIN tour_sessions ts ON r.session_id = ts.id "
                    + "JOIN tour_programs tp ON ts.program_id = tp.id "
                    + "LEFT JOIN visit_records v ON v.reservation_id = r.id ";

    @Override
    public Reservation save(Reservation r) {
        if (r.getId() == 0) {
            return insert(r);
        }
        return update(r);
    }

    @Override
    public Optional<Reservation> findById(int id) {
        String sql = "SELECT id, session_id, guest_id, party_size, status, created_at FROM reservations WHERE id=?";
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
    public int countByFilters(String search, String statusFilter) {
        String where = buildWhereClause();
        String sql = "SELECT COUNT(*) " + BASE_FROM + where;
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            bindFilterParams(ps, search, statusFilter, 1);
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
    public List<ReservationListRow> findPage(
            String search, String statusFilter, String sortColumn, boolean ascending, int offset, int limit) {
        String orderCol = mapSortColumn(sortColumn);
        String dir = ascending ? "ASC" : "DESC";
        String where = buildWhereClause();
        String sql = "SELECT r.id AS reservation_id, g.name AS guest_name, g.email AS guest_email, "
                + "tp.title AS program_title, ts.start_time AS session_start, r.party_size, r.status AS res_status, "
                + "CASE WHEN v.id IS NULL THEN '—' ELSE (v.status || ' (' || v.attended_count || ')') END AS visit_info "
                + BASE_FROM
                + where
                + " ORDER BY "
                + orderCol
                + " "
                + dir
                + " LIMIT ? OFFSET ?";
        List<ReservationListRow> list = new ArrayList<>();
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = bindFilterParams(ps, search, statusFilter, 1);
            ps.setInt(idx, limit);
            ps.setInt(idx + 1, offset);
            var rs = ps.executeQuery();
            while (rs.next()) {
                list.add(
                        ReservationListRow.builder()
                                .reservationId(rs.getInt("reservation_id"))
                                .guestName(rs.getString("guest_name"))
                                .guestEmail(rs.getString("guest_email"))
                                .programTitle(rs.getString("program_title"))
                                .sessionStart(rs.getString("session_start"))
                                .partySize(rs.getInt("party_size"))
                                .reservationStatus(rs.getString("res_status"))
                                .visitInfo(rs.getString("visit_info"))
                                .build());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public void updateStatus(int id, String status) {
        String sql = "UPDATE reservations SET status=? WHERE id=?";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildWhereClause() {
        return "WHERE (? = 1 OR lower(g.name) LIKE lower(?) OR lower(g.email) LIKE lower(?) OR lower(tp.title) LIKE lower(?)) "
                + "AND (? = 1 OR r.status = ?)";
    }

    private int bindFilterParams(PreparedStatement ps, String search, String statusFilter, int startIndex)
            throws SQLException {
        String s = search == null ? "" : search.trim();
        String likePat = s.isEmpty() ? "" : "%" + s + "%";
        int searchDisabled = s.isEmpty() ? 1 : 0;

        String st = statusFilter == null || statusFilter.isBlank() || "Всі".equals(statusFilter) ? "" : statusFilter;
        int statusDisabled = st.isEmpty() ? 1 : 0;

        ps.setInt(startIndex, searchDisabled);
        ps.setString(startIndex + 1, likePat);
        ps.setString(startIndex + 2, likePat);
        ps.setString(startIndex + 3, likePat);
        ps.setInt(startIndex + 4, statusDisabled);
        ps.setString(startIndex + 5, st.isEmpty() ? "" : st);
        return startIndex + 6;
    }

    private String mapSortColumn(String sortColumn) {
        if (sortColumn == null) {
            return "ts.start_time";
        }
        return switch (sortColumn) {
            case "guest" -> "g.name";
            case "party" -> "r.party_size";
            case "status" -> "r.status";
            default -> "ts.start_time";
        };
    }

    private Reservation insert(Reservation r) {
        String sql = "INSERT INTO reservations(session_id, guest_id, party_size, status, created_at) VALUES(?,?,?,?,?)";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getSessionId());
            ps.setInt(2, r.getGuestId());
            ps.setInt(3, r.getPartySize());
            ps.setString(4, r.getStatus());
            ps.setString(5, r.getCreatedAt());
            ps.executeUpdate();
            var gk = ps.getGeneratedKeys();
            if (gk.next()) {
                return r.toBuilder().id(gk.getInt(1)).build();
            }
            throw new RuntimeException("No generated id");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Reservation update(Reservation r) {
        String sql = "UPDATE reservations SET session_id=?, guest_id=?, party_size=?, status=?, created_at=? WHERE id=?";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, r.getSessionId());
            ps.setInt(2, r.getGuestId());
            ps.setInt(3, r.getPartySize());
            ps.setString(4, r.getStatus());
            ps.setString(5, r.getCreatedAt());
            ps.setInt(6, r.getId());
            ps.executeUpdate();
            return r;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Reservation map(ResultSet rs) throws SQLException {
        return Reservation.builder()
                .id(rs.getInt("id"))
                .sessionId(rs.getInt("session_id"))
                .guestId(rs.getInt("guest_id"))
                .partySize(rs.getInt("party_size"))
                .status(rs.getString("status"))
                .createdAt(rs.getString("created_at"))
                .build();
    }
}
