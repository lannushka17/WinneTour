package com.sab.winery.persistence.implementation;

import com.sab.winery.persistence.contract.TourProgramRepository;
import com.sab.winery.persistence.entity.TourProgram;
import com.sab.winery.persistence.utility.PersistenceConfig;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TourProgramRepositoryImpl implements TourProgramRepository {

    @Override
    public List<TourProgram> findAll() {
        return loadWhere(null);
    }

    @Override
    public List<TourProgram> findAllActive() {
        return loadWhere("WHERE active=1");
    }

    private List<TourProgram> loadWhere(String clause) {
        List<TourProgram> list = new ArrayList<>();
        String sql = "SELECT id, winery_id, title, description, duration_minutes, default_max_guests, image_path, active FROM tour_programs "
                + (clause != null ? clause + " " : "")
                + "ORDER BY title";
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
    public Optional<TourProgram> findById(int id) {
        String sql =
                "SELECT id, winery_id, title, description, duration_minutes, default_max_guests, image_path, active FROM tour_programs WHERE id=?";
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
    public TourProgram save(TourProgram p) {
        if (p.getId() == 0) {
            return insert(p);
        }
        return update(p);
    }

    @Override
    public void setActive(int id, boolean active) {
        String sql = "UPDATE tour_programs SET active=? WHERE id=?";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private TourProgram insert(TourProgram p) {
        String sql = "INSERT INTO tour_programs(winery_id, title, description, duration_minutes, default_max_guests, image_path, active) "
                + "VALUES(?,?,?,?,?,?,?)";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getWineryId());
            ps.setString(2, p.getTitle());
            ps.setString(3, p.getDescription());
            ps.setInt(4, p.getDurationMinutes());
            ps.setInt(5, p.getDefaultMaxGuests());
            ps.setString(6, p.getImagePath());
            ps.setInt(7, p.isActive() ? 1 : 0);
            ps.executeUpdate();
            var gk = ps.getGeneratedKeys();
            if (gk.next()) {
                return p.toBuilder().id(gk.getInt(1)).build();
            }
            throw new RuntimeException("No generated id");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private TourProgram update(TourProgram p) {
        String sql = "UPDATE tour_programs SET winery_id=?, title=?, description=?, duration_minutes=?, default_max_guests=?, image_path=?, active=? "
                + "WHERE id=?";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, p.getWineryId());
            ps.setString(2, p.getTitle());
            ps.setString(3, p.getDescription());
            ps.setInt(4, p.getDurationMinutes());
            ps.setInt(5, p.getDefaultMaxGuests());
            ps.setString(6, p.getImagePath());
            ps.setInt(7, p.isActive() ? 1 : 0);
            ps.setInt(8, p.getId());
            ps.executeUpdate();
            return p;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private TourProgram map(ResultSet rs) throws SQLException {
        return TourProgram.builder()
                .id(rs.getInt("id"))
                .wineryId(rs.getInt("winery_id"))
                .title(rs.getString("title"))
                .description(rs.getString("description"))
                .durationMinutes(rs.getInt("duration_minutes"))
                .defaultMaxGuests(rs.getInt("default_max_guests"))
                .imagePath(rs.getString("image_path"))
                .active(rs.getInt("active") == 1)
                .build();
    }
}
