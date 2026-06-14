package com.jellymc.cosmetics.spigot.database;

import com.jellymc.cosmetics.core.database.AuctionRepository;
import com.jellymc.cosmetics.core.database.CosmeticRepository;
import com.jellymc.cosmetics.core.database.DatabaseConnector;
import com.jellymc.cosmetics.spigot.JellyCosmeticsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;

public class SpigotDatabaseManager {

    private final JellyCosmeticsPlugin plugin;
    private DatabaseConnector databaseConnector;
    private CosmeticRepository cosmeticRepository;
    private AuctionRepository auctionRepository;

    public SpigotDatabaseManager(JellyCosmeticsPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        FileConfiguration config = plugin.getConfig();

        // Check if MySQL is enabled
        boolean useMySQL = config.getBoolean("database.mysql.enabled", false);

        if (useMySQL) {
            // Use MySQL if enabled
            String host = config.getString("database.mysql.host", "localhost");
            int port = config.getInt("database.mysql.port", 3306);
            String database = config.getString("database.mysql.name", "jellymc_cosmetics");
            String username = config.getString("database.mysql.username", "root");
            String password = config.getString("database.mysql.password", "");

            databaseConnector = new DatabaseConnector(
                    plugin.getLogger(),
                    host,
                    port,
                    database,
                    username,
                    password
            );

            plugin.getLogger().info("Using MySQL database");
        } else {
            // Use SQLite by default
            databaseConnector = new DatabaseConnector(
                    plugin.getLogger(),
                    plugin.getDataFolder()
            );

            plugin.getLogger().info("Using SQLite database");
        }

        cosmeticRepository = new CosmeticRepository(plugin.getLogger(), databaseConnector);
        auctionRepository = new AuctionRepository(plugin.getLogger(), databaseConnector);

        // Schedule cleanup task for expired auction listings
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin,
                () -> auctionRepository.cleanupExpiredListings(),
                20L * 60, // 1 minute delay
                20L * 60 * 30); // Run every 30 minutes
    }

    public Connection getConnection() throws SQLException {
        return databaseConnector.getConnection();
    }

    public CosmeticRepository getCosmeticRepository() {
        return cosmeticRepository;
    }

    public AuctionRepository getAuctionRepository() {
        return auctionRepository;
    }

    public void shutdown() {
        if (databaseConnector != null) {
            databaseConnector.shutdown();
        }
    }
}
