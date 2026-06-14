package com.jellymc.cosmetics.velocity.database;

import com.jellymc.cosmetics.core.database.DatabaseConnector;
import com.jellymc.cosmetics.velocity.JellyCosmeticsVelocity;
import ninja.leaping.configurate.ConfigurationNode;

import java.sql.Connection;
import java.sql.SQLException;

public class VelocityDatabaseManager {

    private final JellyCosmeticsVelocity plugin;
    private DatabaseConnector databaseConnector;

    public VelocityDatabaseManager(JellyCosmeticsVelocity plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        ConfigurationNode config = plugin.getConfigManager().getConfig();

        // Check if MySQL is enabled
        boolean useMySQL = config.getNode("database", "mysql", "enabled").getBoolean(false);

        if (useMySQL) {
            // Use MySQL if enabled
            String host = config.getNode("database", "mysql", "host").getString("localhost");
            int port = config.getNode("database", "mysql", "port").getInt(3306);
            String database = config.getNode("database", "mysql", "name").getString("jellymc_cosmetics");
            String username = config.getNode("database", "mysql", "username").getString("root");
            String password = config.getNode("database", "mysql", "password").getString("");

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
                    plugin.getDataDirectory().toFile()
            );

            plugin.getLogger().info("Using SQLite database");
        }
    }

    public Connection getConnection() throws SQLException {
        return databaseConnector.getConnection();
    }

    public void shutdown() {
        if (databaseConnector != null) {
            databaseConnector.shutdown();
        }
    }
}