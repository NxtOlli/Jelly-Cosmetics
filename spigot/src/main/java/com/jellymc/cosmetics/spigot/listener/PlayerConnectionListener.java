package com.jellymc.cosmetics.spigot.listener;

import com.jellymc.cosmetics.core.model.NamePaint;
import com.jellymc.cosmetics.spigot.JellyCosmeticsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {
    private final JellyCosmeticsPlugin plugin;

    public PlayerConnectionListener(JellyCosmeticsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Apply resource pack if needed
        if (!plugin.getResourcePackManager().hasResourcePack(player.getUniqueId())) {
            plugin.getResourcePackManager().applyResourcePack(player);
        }

        // Apply name paint if player has one
        String namePaintId = plugin.getDatabaseManager().getCosmeticRepository()
                .getPlayerNamePaint(player.getUniqueId());

        if (namePaintId != null) {
            NamePaint namePaint = plugin.getConfigManager().getNamePaintById(namePaintId);
            if (namePaint != null) {
                // Check permission
                if (!namePaint.getPermission().isEmpty() && !player.hasPermission(namePaint.getPermission())) {
                    player.sendMessage("§cYou no longer have permission to use this name paint.");
                    plugin.getDatabaseManager().getCosmeticRepository().setPlayerNamePaint(player.getUniqueId(), null);
                } else {
                    // Apply name paint
                    String formattedName = namePaint.formatName(player.getName());
                    player.setDisplayName(formattedName);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Clean up resource pack status
        plugin.getResourcePackManager().resetPlayerPackStatus(player.getUniqueId());
    }
}
