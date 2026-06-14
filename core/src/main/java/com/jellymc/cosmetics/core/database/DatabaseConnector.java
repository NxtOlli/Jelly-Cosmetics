package com.jellymc.cosmetics.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseConnector {
    private final Logger logger;
    private HikariDataSource dataSource;
    private final boolean useSQLite;
    private final File dataFolder;

    /**
     * Creates a new database connector using SQLite
     *
     * @param logger The logger to use
     * @param dataFolder The plugin data folder
     */
    public DatabaseConnector(Logger logger, File dataFolder) {
        this.logger = logger;
        this.dataFolder = dataFolder;
        this.useSQLite = true;

        initializeSQLite();
    }

    /**
     * Creates a new database connector using MySQL
     *
     * @param logger The logger to use
     * @param host The MySQL host
     * @param port The MySQL port
     * @param database The MySQL database name
     * @param username The MySQL username
     * @param password The MySQL password
     */
    public DatabaseConnector(Logger logger, String host, int port, String database,
                             String username, String password) {
        this.logger = logger;
        this.dataFolder = null;
        this.useSQLite = false;

        initializeMySQL(host, port, database, username, password);
    }

    private void initializeSQLite() {
        try {
            // Ensure data folder exists
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            File dbFile = new File(dataFolder, "cosmetics.db");

            // Configure connection pool for SQLite
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setConnectionTestQuery("SELECT 1");
            config.setMaximumPoolSize(1); // SQLite only supports one connection at a time

            // Additional SQLite-specific settings
            config.addDataSourceProperty("pragma.synchronous", "normal");
            config.addDataSourceProperty("pragma.journal_mode", "wal");

            dataSource = new HikariDataSource(config);
            createTables();
            logger.info("Successfully connected to SQLite database!");
        } catch (Exception e) {
            logger.severe("Failed to connect to SQLite database: " + e.getMessage());
        }
    }

    private void initializeMySQL(String host, int port, String database, String username, String password) {
        try {
            // Configure connection pool for MySQL
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(10);

            // Additional MySQL-specific settings
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");

            dataSource = new HikariDataSource(config);
            createTables();
            logger.info("Successfully connected to MySQL database!");
        } catch (Exception e) {
            logger.severe("Failed to connect to MySQL database: " + e.getMessage());
        }
    }

    private void createTables() {
        try (Connection conn = getConnection()) {
            // Player Cosmetics Table
            String playerCosmeticsTable = useSQLite
                    ? "CREATE TABLE IF NOT EXISTS player_cosmetics (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "cosmetic_id VARCHAR(64) NOT NULL, " +
                    "equipped BOOLEAN DEFAULT 0, " +
                    "obtained_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE(player_uuid, cosmetic_id))"
                    : "CREATE TABLE IF NOT EXISTS player_cosmetics (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "cosmetic_id VARCHAR(64) NOT NULL, " +
                    "equipped BOOLEAN DEFAULT FALSE, " +
                    "obtained_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE KEY unique_player_cosmetic (player_uuid, cosmetic_id))";

            try (PreparedStatement stmt = conn.prepareStatement(playerCosmeticsTable)) {
                stmt.executeUpdate();
            }

            // Auction Listings Table
            String auctionListingsTable = useSQLite
                    ? "CREATE TABLE IF NOT EXISTS auction_listings (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "seller_uuid VARCHAR(36) NOT NULL, " +
                    "cosmetic_id VARCHAR(64) NOT NULL, " +
                    "price DECIMAL(10,2) NOT NULL, " +
                    "listed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "expires_at TIMESTAMP NOT NULL, " +
                    "status VARCHAR(10) DEFAULT 'ACTIVE')"
                    : "CREATE TABLE IF NOT EXISTS auction_listings (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "seller_uuid VARCHAR(36) NOT NULL, " +
                    "cosmetic_id VARCHAR(64) NOT NULL, " +
                    "price DECIMAL(10,2) NOT NULL, " +
                    "listed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "expires_at TIMESTAMP NOT NULL, " +
                    "status ENUM('ACTIVE', 'SOLD', 'EXPIRED', 'CANCELLED') DEFAULT 'ACTIVE')";

            try (PreparedStatement stmt = conn.prepareStatement(auctionListingsTable)) {
                stmt.executeUpdate();
            }

            // Auction Transactions Table
            String auctionTransactionsTable = useSQLite
                    ? "CREATE TABLE IF NOT EXISTS auction_transactions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "listing_id INTEGER NOT NULL, " +
                    "buyer_uuid VARCHAR(36) NOT NULL, " +
                    "transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (listing_id) REFERENCES auction_listings(id))"
                    : "CREATE TABLE IF NOT EXISTS auction_transactions (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "listing_id INT NOT NULL, " +
                    "buyer_uuid VARCHAR(36) NOT NULL, " +
                    "transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (listing_id) REFERENCES auction_listings(id))";

            try (PreparedStatement stmt = conn.prepareStatement(auctionTransactionsTable)) {
                stmt.executeUpdate();
            }

            // Player Preferences Table
            String playerPreferencesTable = useSQLite
                    ? "CREATE TABLE IF NOT EXISTS player_preferences (" +
                    "player_uuid VARCHAR(36) PRIMARY KEY, " +
                    "chat_color_id VARCHAR(64), " +
                    "name_paint_id VARCHAR(64), " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
                    : "CREATE TABLE IF NOT EXISTS player_preferences (" +
                    "player_uuid VARCHAR(36) PRIMARY KEY, " +
                    "chat_color_id VARCHAR(64), " +
                    "name_paint_id VARCHAR(64), " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)";

            try (PreparedStatement stmt = conn.prepareStatement(playerPreferencesTable)) {
                stmt.executeUpdate();
            }

            // Cosmetics Table (for type information)
            String cosmeticsTable = "CREATE TABLE IF NOT EXISTS cosmetics (" +
                    "id VARCHAR(64) PRIMARY KEY, " +
                    "name VARCHAR(64) NOT NULL, " +
                    "type VARCHAR(32) NOT NULL, " +
                    "rarity VARCHAR(32) NOT NULL, " +
                    "tradeable BOOLEAN DEFAULT 1)";

            try (PreparedStatement stmt = conn.prepareStatement(cosmeticsTable)) {
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            logger.severe("Failed to create database tables: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public boolean isUsingSQLite() {
        return useSQLite;
    }
}
