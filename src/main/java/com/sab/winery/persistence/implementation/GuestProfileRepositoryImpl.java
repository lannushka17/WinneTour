package com.sab.winery.persistence.implementation;

import com.sab.winery.persistence.contract.GuestProfileRepository;
import com.sab.winery.persistence.entity.GuestProfile;
import com.sab.winery.persistence.utility.PersistenceConfig;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GuestProfileRepositoryImpl implements GuestProfileRepository {

    @Override
    public Optional<GuestProfile> findByEmailIgnoreCase(String email) {
        String sql = "SELECT id, name, email, phone FROM guest_profiles WHERE lower(email)=lower(?)";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim());
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
    public Optional<GuestProfile> findById(int id) {
        String sql = "SELECT id, name, email, phone FROM guest_profiles WHERE id=?";
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
    public List<GuestProfile> findAllOrderByName() {
        List<GuestProfile> list = new ArrayList<>();
        String sql = "SELECT id, name, email, phone FROM guest_profiles ORDER BY name COLLATE NOCASE";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                var rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public GuestProfile save(GuestProfile g) {
        if (g.getId() == 0) {
            return insert(g);
        }
        return update(g);
    }

    @Override
    public void deleteById(int id) {
        String sql = "DELETE FROM guest_profiles WHERE id=?";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int countActiveReservationsForGuest(int guestId) {
        String sql = "SELECT COUNT(*) FROM reservations WHERE guest_id=? AND status='CONFIRMED'";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, guestId);
            var rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private GuestProfile insert(GuestProfile g) {
        String sql = "INSERT INTO guest_profiles(name, email, phone) VALUES(?,?,?)";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, g.getName());
            ps.setString(2, g.getEmail());
            ps.setString(3, g.getPhone());
            ps.executeUpdate();
            var gk = ps.getGeneratedKeys();
            if (gk.next()) {
                return g.toBuilder().id(gk.getInt(1)).build();
            }
            throw new RuntimeException("No generated id");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private GuestProfile update(GuestProfile g) {
        String sql = "UPDATE guest_profiles SET name=?, email=?, phone=? WHERE id=?";
        try (var con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, g.getName());
            ps.setString(2, g.getEmail());
            ps.setString(3, g.getPhone());
            ps.setInt(4, g.getId());
            ps.executeUpdate();
            return g;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private GuestProfile map(ResultSet rs) throws SQLException {
        return GuestProfile.builder()
                .id(rs.getInt("id"))
                .name(rs.getString("name"))
                .email(rs.getString("email"))
                .phone(rs.getString("phone"))
                .build();
    }
}
