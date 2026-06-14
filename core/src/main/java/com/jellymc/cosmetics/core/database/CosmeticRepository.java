package com.jellymc.cosmetics.core.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class CosmeticRepository {
    private final Logger logger;
    private final DatabaseConnector databaseConnector;
    private final boolean usingSQLite;

    public CosmeticRepository(Logger logger, DatabaseConnector databaseConnector) {
        this.logger = logger;
        this.databaseConnector = databaseConnector;
        this.usingSQLite = databaseConnector.isUsingSQLite();
    }

    public List<String> getPlayerCosmeticIds(UUID playerUuid) {
        List<String> cosmeticIds = new ArrayList<>();

        try (Connection conn = databaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT cosmetic_id FROM player_cosmetics WHERE player_uuid = ?")) {

            stmt.setString(1, playerUuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    cosmeticIds.add(rs.getString("cosmetic_id"));
                }
            }

        } catch (SQLException e) {
            logger.severe("Failed to get player cosmetics: " + e.getMessage());
        }

        return cosmeticIds;
    }

    public List<String> getPlayerCosmeticIdsByType(UUID playerUuid, String type) {
        List<String> cosmeticIds = new ArrayList<>();

        try (Connection conn = databaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT pc.cosmetic_id FROM player_cosmetics pc " +
                             "JOIN cosmetics c ON pc.cosmetic_id = c.id " +
                             "WHERE pc.player_uuid = ? AND c.type = ?")) {

            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, type);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    cosmeticIds.add(rs.getString("cosmetic_id"));
                }
            }

        } catch (SQLException e) {
            logger.severe("Failed to get player cosmetics by type: " + e.getMessage());
        }

        return cosmeticIds;
    }

    public List<String> getPlayerEquippedCosmeticIds(UUID playerUuid) {
        List<String> equippedCosmeticIds = new ArrayList<>();

        try (Connection conn = databaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT cosmetic_id FROM player_cosmetics WHERE player_uuid = ? AND equipped = TRUE")) {

            stmt.setString(1, playerUuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    equippedCosmeticIds.add(rs.getString("cosmetic_id"));
                }
            }

        } catch (SQLException e) {
            logger.severe("Failed to get player equipped cosmetics: " + e.getMessage());
        }

        return equippedCosmeticIds;
    }

    public boolean playerOwnsCosmetic(UUID playerUuid, String cosmeticId) {
        try (Connection conn = databaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT 1 FROM player_cosmetics WHERE player_uuid = ? AND cosmetic_id = ?")) {

            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, cosmeticId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            logger.severe("Failed to check if player owns cosmetic: " + e.getMessage());
            return false;
        }
    }

    public boolean isEquipped(UUID playerUuid, String cosmeticId) {
        try (Connection conn = databaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT equipped FROM player_cosmetics WHERE player_uuid = ? AND cosmetic_id = ?")) {

            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, cosmeticId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("equipped");
                }
                return false;
            }

        } catch (SQLException e) {
            logger.severe("Failed to check if cosmetic is equipped: " + e.getMessage());
            return false;
        }
    }

    public void giveCosmetic(UUID playerUuid, String cosmeticId) {
        try (Connection conn = databaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO player_cosmetics (player_uuid, cosmetic_id) VALUES (?, ?)")) {

            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, cosmeticId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            logger.severe("Failed to give cosmetic to player: " + e.getMessage());
        }
    }

    public void equipCosmetic(UUID playerUuid, String cosmeticId) {
        try (Connection conn = databaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE player_cosmetics SET equipped = TRUE WHERE player_uuid = ? AND cosmetic_id = ?")) {

            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, cosmeticId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            logger.severe("Failed to equip cosmetic: " + e.getMessage());
        }
    }

    public void unequipCosmetic(UUID playerUuid, String cosmeticId) {
        try (Connection conn = databaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE player_cosmetics SET equipped = FALSE WHERE player_uuid = ? AND cosmetic_id = ?")) {

            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, cosmeticId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            logger.severe("Failed to unequip cosmetic: " + e.getMessage());
        }
    }

    public String getPlayerChatColor(UUID playerUuid) {
        try (Connection conn = databaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT chat_color_id FROM player_preferences WHERE player_uuid = ?")) {

            stmt.setString(1, playerUuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("chat_color_id");
                }
            }

        } catch (SQLException e) {
            logger.severe("Failed to get player chat color: " + e.getMessage());
        }

        return null;
    }

    public String getPlayerNamePaint(UUID playerUuid) {
        try (Connection conn = databaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT name_paint_id FROM player_preferences WHERE player_uuid = ?")) {

            stmt.setString(1, playerUuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name_paint_id");
                }
            }

        } catch (SQLException e) {
            logger.severe("Failed to get player name paint: " + e.getMessage());
        }

        return null;
    }

    public void setPlayerChatColor(UUID playerUuid, String chatColorId) {
        try (Connection conn = databaseConnector.getConnection()) {
            String sql;

            if (usingSQLite) {
                // SQLite version using INSERT OR REPLACE
                sql = "INSERT OR REPLACE INTO player_preferences (player_uuid, chat_color_id) VALUES (?, ?)";
            } else {
                // MySQL version using ON DUPLICATE KEY UPDATE
                sql = "INSERT INTO player_preferences (player_uuid, chat_color_id) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE chat_color_id = ?";
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, chatColorId);

                if (!usingSQLite) {
                    // Only MySQL needs this third parameter
                    stmt.setString(3, chatColorId);
                }

                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            logger.severe("Failed to set player chat color: " + e.getMessage());
        }
    }

    public void setPlayerNamePaint(UUID playerUuid, String namePaintId) {
        try (Connection conn = databaseConnector.getConnection()) {
            String sql;

            if (usingSQLite) {
                // SQLite version using INSERT OR REPLACE
                sql = "INSERT OR REPLACE INTO player_preferences (player_uuid, name_paint_id) VALUES (?, ?)";
            } else {
                // MySQL version using ON DUPLICATE KEY UPDATE
                sql = "INSERT INTO player_preferences (player_uuid, name_paint_id) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE name_paint_id = ?";
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, namePaintId);

                if (!usingSQLite) {
                    // Only MySQL needs this third parameter
                    stmt.setString(3, namePaintId);
                }

                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            logger.severe("Failed to set player name paint: " + e.getMessage());
        }
    }

    public void removeCosmetic(UUID playerUuid, String cosmeticId) {
        try (Connection conn = databaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM player_cosmetics WHERE player_uuid = ? AND cosmetic_id = ?")) {

            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, cosmeticId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            logger.severe("Failed to remove cosmetic from player: " + e.getMessage());
        }
    }
}
