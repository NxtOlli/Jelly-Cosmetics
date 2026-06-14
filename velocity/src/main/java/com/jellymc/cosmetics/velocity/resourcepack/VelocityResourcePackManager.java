package com.jellymc.cosmetics.velocity.resourcepack;

import com.jellymc.cosmetics.velocity.JellyCosmeticsVelocity;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VelocityResourcePackManager {

    private final JellyCosmeticsVelocity plugin;
    private final Map<UUID, Boolean> playerPackStatus = new ConcurrentHashMap<>();
    private String resourcePackUrl;
    private String resourcePackHash;
    private boolean forceResourcePack;
    private Component resourcePackPrompt;
    private List<String> hubServers;

    public VelocityResourcePackManager(JellyCosmeticsVelocity plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        resourcePackUrl = plugin.getConfigManager().getConfig().getNode("resourcepack", "url").getString("");
        resourcePackHash = plugin.getConfigManager().getConfig().getNode("resourcepack", "hash").getString("");
        forceResourcePack = plugin.getConfigManager().getConfig().getNode("resourcepack", "force").getBoolean(false);
        resourcePackPrompt = Component.text(plugin.getConfigManager().getConfig().getNode("resourcepack", "prompt").getString("Please accept the resource pack to see cosmetics!"));
        hubServers = plugin.getConfigManager().getHubServers();
    }

    /**
     * Tracks when a player joins the hub server to apply the resource pack
     *
     * @param player The player who joined
     * @param server The server they joined
     */
    public void onPlayerJoinServer(Player player, RegisteredServer server) {
        // Check if this is a hub server
        String serverName = server.getServerInfo().getName();
        if (isHubServer(serverName)) {
            // Apply resource pack if player doesn't have it yet
            if (!playerPackStatus.getOrDefault(player.getUniqueId(), false)) {
                applyResourcePack(player);
            }
        }
    }

    /**
     * Applies the resource pack to a player
     *
     * @param player The player to apply the pack to
     */
    public void applyResourcePack(Player player) {
        // Resource pack application would be handled by the Spigot server
        // We just track that we've requested it
        playerPackStatus.put(player.getUniqueId(), true);
    }

    /**
     * Checks if a server is a hub server
     *
     * @param serverName The server name to check
     * @return True if it's a hub server
     */
    private boolean isHubServer(String serverName) {
        return hubServers.contains(serverName);
    }

    /**
     * Marks a player as not having the resource pack
     *
     * @param playerId The UUID of the player
     */
    public void resetPlayerPackStatus(UUID playerId) {
        playerPackStatus.remove(playerId);
    }

    /**
     * Gets whether a player has the resource pack
     *
     * @param playerId The UUID of the player
     * @return True if the player has the resource pack
     */
    public boolean hasResourcePack(UUID playerId) {
        return playerPackStatus.getOrDefault(playerId, false);
    }
}
