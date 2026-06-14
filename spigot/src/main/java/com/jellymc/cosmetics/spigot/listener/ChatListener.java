package com.jellymc.cosmetics.spigot.listener;

import com.jellymc.cosmetics.core.model.NamePaint;
import com.jellymc.cosmetics.spigot.JellyCosmeticsPlugin;
import com.jellymc.cosmetics.core.model.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatListener implements Listener {
    private final JellyCosmeticsPlugin plugin;
    private final Map<UUID, Long> lastAnimationTick = new HashMap<>();

    public ChatListener(JellyCosmeticsPlugin plugin) {
        this.plugin = plugin;

        // Start animation tick counter
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                updatePlayerNamePaint(player);
            }
        }, 1L, plugin.getConfig().getInt("name_paints.update_interval", 5));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Apply chat color if player has one
        String chatColorId = plugin.getDatabaseManager().getCosmeticRepository()
                .getPlayerChatColor(player.getUniqueId());

        if (chatColorId != null) {
            ChatColor chatColor = plugin.getConfigManager().getChatColorById(chatColorId);
            if (chatColor != null) {
                // Check permission
                if (!chatColor.getPermission().isEmpty() && !player.hasPermission(chatColor.getPermission())) {
                    player.sendMessage("§cYou no longer have permission to use this chat color.");
                    plugin.getDatabaseManager().getCosmeticRepository().setPlayerChatColor(player.getUniqueId(), null);
                } else {
                    // Apply chat color
                    String colorCode;
                    if (chatColor.isAnimated()) {
                        // For animated chat colors, we use the current frame
                        long tick = plugin.getServer().getWorld(player.getWorld().getName()).getTime();
                        colorCode = chatColor.getCurrentFrame(tick);
                    } else {
                        colorCode = chatColor.getColorCode();
                    }

                    event.setMessage(colorCode + event.getMessage());
                }
            }
        }
    }

    /**
     * Updates a player's display name based on their name paint
     *
     * @param player The player to update
     */
    public void updatePlayerNamePaint(Player player) {
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
                    player.setDisplayName(player.getName());
                } else {
                    // Apply name paint
                    String formattedName;
                    if (namePaint.isAnimated()) {
                        // For animated name paints, we use the current frame
                        long tick = plugin.getServer().getWorld(player.getWorld().getName()).getTime();
                        lastAnimationTick.put(player.getUniqueId(), tick);
                        formattedName = namePaint.formatNameWithFrame(player.getName(), tick);
                    } else {
                        formattedName = namePaint.formatName(player.getName());
                    }

                    player.setDisplayName(formattedName);
                }
            }
        }
    }
}
