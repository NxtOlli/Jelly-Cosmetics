package com.jellymc.cosmetics.spigot.resourcepack;

import com.jellymc.cosmetics.spigot.JellyCosmeticsPlugin;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ResourcePackManager {
    private final JellyCosmeticsPlugin plugin;
    private final Map<UUID, Boolean> playerPackStatus = new HashMap<>();
    private String resourcePackUrl;
    private String resourcePackHash;
    private boolean forceResourcePack;

    public ResourcePackManager(JellyCosmeticsPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        resourcePackUrl = plugin.getConfig().getString("resourcepack.url", "");
        resourcePackHash = plugin.getConfig().getString("resourcepack.hash", "");
        forceResourcePack = plugin.getConfig().getBoolean("resourcepack.force", false);
    }

    /**
     * Applies the resource pack to a player
     *
     * @param player The player to apply the pack to
     */
    public void applyResourcePack(Player player) {
        if (resourcePackUrl.isEmpty()) {
            plugin.getLogger().warning("Resource pack URL is not configured!");
            return;
        }

        // Apply resource pack
        if (resourcePackHash.isEmpty()) {
            player.setResourcePack(resourcePackUrl);
        } else {
            try {
                byte[] hashBytes = hexStringToByteArray(resourcePackHash);
                player.setResourcePack(resourcePackUrl, hashBytes);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid resource pack hash: " + resourcePackHash);
                player.setResourcePack(resourcePackUrl);
            }
        }
    }

    /**
     * Marks a player as having accepted the resource pack
     *
     * @param playerId The UUID of the player
     */
    public void setPlayerHasResourcePack(UUID playerId) {
        playerPackStatus.put(playerId, true);
    }

    /**
     * Marks a player as having declined the resource pack
     *
     * @param playerId The UUID of the player
     */
    public void setPlayerDeclinedResourcePack(UUID playerId) {
        playerPackStatus.put(playerId, false);

        // If resource pack is forced, kick the player
        if (forceResourcePack) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                player.kickPlayer("§cYou must accept the resource pack to play on this server!");
            }
        }
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

    /**
     * Converts a hex string to a byte array
     *
     * @param s The hex string
     * @return The byte array
     */
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
