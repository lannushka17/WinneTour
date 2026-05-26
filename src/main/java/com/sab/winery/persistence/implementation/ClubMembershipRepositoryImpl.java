package com.sab.winery.persistence.implementation;

import com.sab.winery.persistence.contract.ClubMembershipRepository;
import com.sab.winery.persistence.entity.ClubMembership;
import com.sab.winery.persistence.entity.ClubTier;
import com.sab.winery.persistence.utility.PersistenceConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClubMembershipRepositoryImpl implements ClubMembershipRepository {

    private static final String SQL_FIND_BY_ID = "SELECT * FROM club_memberships WHERE id=?";
    private static final String SQL_FIND_ALL = "SELECT * FROM club_memberships ORDER BY valid_until DESC";
    private static final String SQL_FIND_ACTIVE_BY_GUEST =
            "SELECT * FROM club_memberships WHERE guest_id=? AND active=1 ORDER BY valid_until DESC LIMIT 1";
    private static final String SQL_INSERT =
            "INSERT INTO club_memberships(guest_id, tier, valid_until, discount_percent, active) VALUES (?,?,?,?,?)";
    private static final String SQL_UPDATE =
            "UPDATE club_memberships SET guest_id=?, tier=?, valid_until=?, discount_percent=?, active=? WHERE id=?";
    private static final String SQL_DELETE = "DELETE FROM club_memberships WHERE id=?";

    @Override
    public Optional<ClubMembership> findById(int id) {
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
    public Optional<ClubMembership> findActiveByGuestId(int guestId) {
        try (Connection con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_FIND_ACTIVE_BY_GUEST)) {
            ps.setInt(1, guestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ClubMembership> findAll() {
        List<ClubMembership> list = new ArrayList<>();
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
    public ClubMembership save(ClubMembership m) {
        return m.getId() == 0 ? insert(m) : update(m);
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

    private ClubMembership insert(ClubMembership m) {
        try (Connection con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getGuestId());
            ps.setString(2, m.getTier().name());
            ps.setString(3, m.getValidUntil().toString());
            ps.setInt(4, m.getDiscountPercent());
            ps.setInt(5, m.isActive() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return m.toBuilder().id(keys.getInt(1)).build();
                }
                throw new RuntimeException("Не отримано згенерований ID картки клубу");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private ClubMembership update(ClubMembership m) {
        try (Connection con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {
            ps.setInt(1, m.getGuestId());
            ps.setString(2, m.getTier().name());
            ps.setString(3, m.getValidUntil().toString());
            ps.setInt(4, m.getDiscountPercent());
            ps.setInt(5, m.isActive() ? 1 : 0);
            ps.setInt(6, m.getId());
            ps.executeUpdate();
            return m;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private ClubMembership map(ResultSet rs) throws SQLException {
        return ClubMembership.builder()
                .id(rs.getInt("id"))
                .guestId(rs.getInt("guest_id"))
                .tier(ClubTier.valueOf(rs.getString("tier")))
                .validUntil(LocalDate.parse(rs.getString("valid_until")))
                .discountPercent(rs.getInt("discount_percent"))
                .active(rs.getInt("active") == 1)
                .build();
    }
}
