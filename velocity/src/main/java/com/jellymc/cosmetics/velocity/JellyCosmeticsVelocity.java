package com.jellymc.cosmetics.velocity;

import com.google.inject.Inject;
import com.jellymc.cosmetics.velocity.command.VelocityCosmeticAdminCommand;
import com.jellymc.cosmetics.velocity.command.VelocityCosmeticCommand;
import com.jellymc.cosmetics.velocity.config.VelocityConfigManager;
import com.jellymc.cosmetics.velocity.database.VelocityDatabaseManager;
import com.jellymc.cosmetics.velocity.listener.PlayerConnectionListener;
import com.jellymc.cosmetics.velocity.resourcepack.VelocityResourcePackManager;
import com.jellymc.cosmetics.velocity.sync.VelocitySyncManager;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import java.nio.file.Path;
import java.util.logging.Logger;

@Plugin(
        id = "jellycosmetics",
        name = "JellyCosmetics",
        version = "1.0.0",
        description = "Network-wide cosmetic system for Minecraft servers",
        authors = {"JellyMC"}
)
public class JellyCosmeticsVelocity {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private VelocityConfigManager configManager;
    private VelocityDatabaseManager databaseManager;
    private VelocitySyncManager syncManager;
    private VelocityResourcePackManager resourcePackManager;

    @Inject
    public JellyCosmeticsVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Initialize configuration
        this.configManager = new VelocityConfigManager(this, dataDirectory);
        configManager.loadConfigs();

        // Initialize database connection
        this.databaseManager = new VelocityDatabaseManager(this);
        databaseManager.initialize();

        // Initialize sync system
        this.syncManager = new VelocitySyncManager(this);
        syncManager.initialize();

        // Initialize resource pack manager
        this.resourcePackManager = new VelocityResourcePackManager(this);

        // Register event listeners
        server.getEventManager().register(this, new PlayerConnectionListener(this));

        // Register commands
        server.getCommandManager().register("cosmetic", new VelocityCosmeticCommand(this));
        server.getCommandManager().register("cosmeticadmin", new VelocityCosmeticAdminCommand(this));

        // Register plugin message channels
        server.getChannelRegistrar().register();

        logger.info("JellyMC Cosmetics Plugin has been enabled!");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        // Close database connections
        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        // Close sync connections
        if (syncManager != null) {
            syncManager.shutdown();
        }

        logger.info("JellyMC Cosmetics Plugin has been disabled!");
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public VelocityConfigManager getConfigManager() {
        return configManager;
    }

    public VelocityDatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public VelocitySyncManager getSyncManager() {
        return syncManager;
    }

    public VelocityResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }
}