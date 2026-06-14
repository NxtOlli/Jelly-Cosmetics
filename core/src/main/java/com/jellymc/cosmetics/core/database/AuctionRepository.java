package com.jellymc.cosmetics.core.database;

import com.jellymc.cosmetics.core.model.AuctionListing;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class AuctionRepository {
    private final Logger logger;
    private final DatabaseConnector databaseConnector;
    private final boolean usingSQLite;

    public AuctionRepository(Logger logger, DatabaseConnector databaseConnector) {
        this.logger = logger;
        this.databaseConnector = databaseConnector;
        this.usingSQLite = databaseConnector.isUsingSQLite();
    }

    /**
     * Gets all active auction listings, optionally filtered by type
     *
     * @param filter The cosmetic type filter (null for all types)
     * @return List of active auction listings
     */
    public List<AuctionListing> getActiveListings(String filter) {
        List<AuctionListing> listings = new ArrayList<>();

        try (Connection conn = databaseConnector.getConnection()) {
            PreparedStatement stmt;

            // SQLite doesn't support the NOW() function, use datetime('now') instead
            String timeCheck = usingSQLite
                    ? "al.expires_at > datetime('now')"
                    : "al.expires_at > NOW()";

            if (filter == null) {
                stmt = conn.prepareStatement(
                        "SELECT al.*, c.type FROM auction_listings al " +
                                "JOIN cosmetics c ON al.cosmetic_id = c.id " +
                                "WHERE al.status = 'ACTIVE' AND " + timeCheck + " " +
                                "ORDER BY al.listed_at DESC");
            } else {
                stmt = conn.prepareStatement(
                        "SELECT al.*, c.type FROM auction_listings al " +
                                "JOIN cosmetics c ON al.cosmetic_id = c.id " +
                                "WHERE al.status = 'ACTIVE' AND " + timeCheck + " AND c.type LIKE ? " +
                                "ORDER BY al.listed_at DESC");
                stmt.setString(1, filter + "%");
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    listings.add(mapResultSetToListing(rs));
                }
            }

        } catch (SQLException e) {
            logger.severe("Failed to get active listings: " + e.getMessage());
        }

        return listings;
    }

    /**
     * Gets all active auction listings for a specific player
     *
     * @param playerUuid The UUID of the player
     * @return List of the player's active auction listings
     */
    public List<AuctionListing> getPlayerListings(UUID playerUuid) {
        List<AuctionListing> listings = new ArrayList<>();

        try (Connection conn = databaseConnector.getConnection()) {
            // SQLite doesn't support the NOW() function, use datetime('now') instead
            String timeCheck = usingSQLite
                    ? "al.expires_at > datetime('now')"
                    : "al.expires_at > NOW()";

            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT al.*, c.type FROM auction_listings al " +
                            "JOIN cosmetics c ON al.cosmetic_id = c.id " +
                            "WHERE al.seller_uuid = ? AND al.status = 'ACTIVE' AND " + timeCheck + " " +
                            "ORDER BY al.listed_at DESC")) {

                stmt.setString(1, playerUuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        listings.add(mapResultSetToListing(rs));
                    }
                }
            }

        } catch (SQLException e) {
            logger.severe("Failed to get player listings: " + e.getMessage());
        }

        return listings;
    }

    /**
     * Gets a specific auction listing by ID
     *
     * @param listingId The ID of the listing
     * @return The auction listing, or null if not found
     */
    public AuctionListing getListing(int listingId) {
        try (Connection conn = databaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT al.*, c.type FROM auction_listings al " +
                             "JOIN cosmetics c ON al.cosmetic_id = c.id " +
                             "WHERE al.id = ?")) {

            stmt.setInt(1, listingId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToListing(rs);
                }
            }

        } catch (SQLException e) {
            logger.severe("Failed to get listing: " + e.getMessage());
        }

        return null;
    }

    /**
     * Creates a new auction listing
     *
     * @param sellerUuid The UUID of the seller
     * @param cosmeticId The ID of the cosmetic being sold
     * @param price The price of the listing
     * @param durationHours How long the listing should last in hours
     * @return The ID of the created listing, or -1 if creation failed
     */
    public int createListing(UUID sellerUuid, String cosmeticId, double price, long durationHours) {
        try (Connection conn = databaseConnector.getConnection()) {
            String sql;

            if (usingSQLite) {
                // SQLite version using datetime function
                sql = "INSERT INTO auction_listings (seller_uuid, cosmetic_id, price, expires_at) " +
                        "VALUES (?, ?, ?, datetime('now', '+' || ? || ' hours'))";
            } else {
                // MySQL version using DATE_ADD
                sql = "INSERT INTO auction_listings (seller_uuid, cosmetic_id, price, expires_at) " +
                        "VALUES (?, ?, ?, DATE_ADD(NOW(), INTERVAL ? HOUR))";
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, sellerUuid.toString());
                stmt.setString(2, cosmeticId);
                stmt.setDouble(3, price);
                stmt.setLong(4, durationHours);

                int affectedRows = stmt.executeUpdate();

                if (affectedRows == 0) {
                    throw new SQLException("Creating listing failed, no rows affected.");
                }

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Creating listing failed, no ID obtained.");
                    }
                }
            }

        } catch (SQLException e) {
            logger.severe("Failed to create listing: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Cancels an auction listing
     *
     * @param listingId The ID of the listing to cancel
     * @param sellerUuid The UUID of the seller (for verification)
     * @return True if the listing was successfully cancelled
     */
    public boolean cancelListing(int listingId, UUID sellerUuid) {
        try (Connection conn = databaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE auction_listings SET status = 'CANCELLED' " +
                             "WHERE id = ? AND seller_uuid = ? AND status = 'ACTIVE'")) {

            stmt.setInt(1, listingId);
            stmt.setString(2, sellerUuid.toString());

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            logger.severe("Failed to cancel listing: " + e.getMessage());
            return false;
        }
    }

    /**
     * Purchases an auction listing
     *
     * @param listingId The ID of the listing to purchase
     * @param buyerUuid The UUID of the buyer
     * @return True if the purchase was successful
     */
    public boolean purchaseListing(int listingId, UUID buyerUuid) {
        Connection conn = null;
        try {
            conn = databaseConnector.getConnection();
            conn.setAutoCommit(false);

            // 1. Get listing and verify it's active
            AuctionListing listing;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM auction_listings WHERE id = ? AND status = 'ACTIVE' AND expires_at > NOW() FOR UPDATE")) {

                stmt.setInt(1, listingId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }

                    listing = new AuctionListing();
                    listing.setId(rs.getInt("id"));
                    listing.setSellerUuid(UUID.fromString(rs.getString("seller_uuid")));
                    listing.setCosmeticId(rs.getString("cosmetic_id"));
                    listing.setPrice(rs.getDouble("price"));
                }
            }

            // 2. Update listing status to SOLD
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE auction_listings SET status = 'SOLD' WHERE id = ?")) {

                stmt.setInt(1, listingId);
                stmt.executeUpdate();
            }

            // 3. Record transaction
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO auction_transactions (listing_id, buyer_uuid) VALUES (?, ?)")) {

                stmt.setInt(1, listingId);
                stmt.setString(2, buyerUuid.toString());
                stmt.executeUpdate();
            }

            // 4. Give cosmetic to buyer
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT IGNORE INTO player_cosmetics (player_uuid, cosmetic_id) VALUES (?, ?)")) {

                stmt.setString(1, buyerUuid.toString());
                stmt.setString(2, listing.getCosmeticId());
                stmt.executeUpdate();
            }

            // 5. Add coins to seller's balance (if economy integration is implemented)
            // This would be handled by your economy plugin integration

            // Commit transaction
            conn.commit();
            return true;

        } catch (SQLException e) {
            logger.severe("Failed to purchase listing: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.severe("Failed to rollback transaction: " + ex.getMessage());
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.severe("Failed to close connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Maps a database result set to an AuctionListing object
     *
     * @param rs The result set to map
     * @return The mapped AuctionListing
     * @throws SQLException If there's an error reading from the result set
     */
    private AuctionListing mapResultSetToListing(ResultSet rs) throws SQLException {
        AuctionListing listing = new AuctionListing();
        listing.setId(rs.getInt("id"));
        listing.setSellerUuid(UUID.fromString(rs.getString("seller_uuid")));
        listing.setCosmeticId(rs.getString("cosmetic_id"));
        listing.setPrice(rs.getDouble("price"));
        listing.setListedAt(rs.getTimestamp("listed_at"));
        listing.setExpiresAt(rs.getTimestamp("expires_at"));
        listing.setStatus(rs.getString("status"));
        listing.setCosmeticType(rs.getString("type"));
        return listing;
    }

    /**
     * Cleans up expired auction listings by marking them as expired
     */
    public void cleanupExpiredListings() {
        try (Connection conn = databaseConnector.getConnection()) {
            // SQLite doesn't support the NOW() function, use datetime('now') instead
            String timeCheck = usingSQLite
                    ? "expires_at <= datetime('now')"
                    : "expires_at <= NOW()";

            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE auction_listings SET status = 'EXPIRED' " +
                            "WHERE status = 'ACTIVE' AND " + timeCheck)) {

                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    logger.info("Cleaned up " + affectedRows + " expired auction listings");
                }
            }

        } catch (SQLException e) {
            logger.severe("Failed to cleanup expired listings: " + e.getMessage());
        }
    }

    /**
     * Gets the total number of active listings
     *
     * @return The count of active listings
     */
    public int getActiveListingsCount() {
        try (Connection conn = databaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM auction_listings WHERE status = 'ACTIVE' AND expires_at > NOW()")) {

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            logger.severe("Failed to get active listings count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Gets the total number of active listings for a specific player
     *
     * @param playerUuid The UUID of the player
     * @return The count of the player's active listings
     */
    public int getPlayerActiveListingsCount(UUID playerUuid) {
        try (Connection conn = databaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM auction_listings " +
                             "WHERE seller_uuid = ? AND status = 'ACTIVE' AND expires_at > NOW()")) {

            stmt.setString(1, playerUuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            logger.severe("Failed to get player active listings count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Gets the sales history for a player
     *
     * @param playerUuid The UUID of the player
     * @param limit Maximum number of records to return
     * @return List of sold auction listings
     */
    public List<AuctionListing> getPlayerSalesHistory(UUID playerUuid, int limit) {
        List<AuctionListing> listings = new ArrayList<>();

        try (Connection conn = databaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT al.*, c.type FROM auction_listings al " +
                             "JOIN cosmetics c ON al.cosmetic_id = c.id " +
                             "WHERE al.seller_uuid = ? AND al.status = 'SOLD' " +
                             "ORDER BY al.listed_at DESC LIMIT ?")) {

            stmt.setString(1, playerUuid.toString());
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    listings.add(mapResultSetToListing(rs));
                }
            }

        } catch (SQLException e) {
            logger.severe("Failed to get player sales history: " + e.getMessage());
        }

        return listings;
    }

    /**
     * Gets the purchase history for a player
     *
     * @param playerUuid The UUID of the player
     * @param limit Maximum number of records to return
     * @return List of purchased auction listings
     */
    public List<AuctionListing> getPlayerPurchaseHistory(UUID playerUuid, int limit) {
        List<AuctionListing> listings = new ArrayList<>();

        try (Connection conn = databaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT al.*, c.type FROM auction_listings al " +
                             "JOIN cosmetics c ON al.cosmetic_id = c.id " +
                             "JOIN auction_transactions at ON al.id = at.listing_id " +
                             "WHERE at.buyer_uuid = ? " +
                             "ORDER BY at.transaction_date DESC LIMIT ?")) {

            stmt.setString(1, playerUuid.toString());
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    listings.add(mapResultSetToListing(rs));
                }
            }

        } catch (SQLException e) {
            logger.severe("Failed to get player purchase history: " + e.getMessage());
        }

        return listings;
    }
}
