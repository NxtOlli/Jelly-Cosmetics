package com.jellymc.cosmetics.spigot;

import com.jellymc.cosmetics.spigot.command.CosmeticAdminCommand;
import com.jellymc.cosmetics.spigot.command.CosmeticCommand;
import com.jellymc.cosmetics.spigot.config.SpigotConfigManager;
import com.jellymc.cosmetics.spigot.database.SpigotDatabaseManager;
import com.jellymc.cosmetics.spigot.listener.ChatListener;
import com.jellymc.cosmetics.spigot.listener.PlayerConnectionListener;
import com.jellymc.cosmetics.spigot.listener.ResourcePackListener;
import com.jellymc.cosmetics.spigot.resourcepack.ResourcePackManager;
import com.jellymc.cosmetics.spigot.sync.SpigotSyncManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class JellyCosmeticsPlugin extends JavaPlugin {

    private static JellyCosmeticsPlugin instance;
    private SpigotConfigManager configManager;
    private SpigotDatabaseManager databaseManager;
    private SpigotSyncManager syncManager;
    private ResourcePackManager resourcePackManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize configuration
        this.configManager = new SpigotConfigManager(this);
        configManager.loadConfigs();

        // Initialize database connection
        this.databaseManager = new SpigotDatabaseManager(this);
        databaseManager.initialize();

        // Initialize sync system (Redis or Plugin Messaging)
        this.syncManager = new SpigotSyncManager(this);
        syncManager.initialize();

        // Initialize resource pack manager
        this.resourcePackManager = new ResourcePackManager(this);

        // Register event listeners
        this.getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        this.getServer().getPluginManager().registerEvents(new ResourcePackListener(this), this);
        this.getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        // Register commands
        this.getCommand("cosmetic").setExecutor(new CosmeticCommand(this));
        this.getCommand("cosmeticadmin").setExecutor(new CosmeticAdminCommand(this));

        getLogger().info("JellyMC Cosmetics Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Close database connections
        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        // Close sync connections
        if (syncManager != null) {
            syncManager.shutdown();
        }

        getLogger().info("JellyMC Cosmetics Plugin has been disabled!");
    }

    public static JellyCosmeticsPlugin getInstance() {
        return instance;
    }

    public SpigotConfigManager getConfigManager() {
        return configManager;
    }

    public SpigotDatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public SpigotSyncManager getSyncManager() {
        return syncManager;
    }

    public ResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }
}
