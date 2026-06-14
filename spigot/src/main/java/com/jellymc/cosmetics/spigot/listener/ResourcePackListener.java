package com.jellymc.cosmetics.spigot.listener;

import com.jellymc.cosmetics.spigot.JellyCosmeticsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

public class ResourcePackListener implements Listener {
    private final JellyCosmeticsPlugin plugin;

    public ResourcePackListener(JellyCosmeticsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        PlayerResourcePackStatusEvent.Status status = event.getStatus();

        switch (status) {
            case SUCCESSFULLY_LOADED:
                plugin.getResourcePackManager().setPlayerHasResourcePack(player.getUniqueId());
                player.sendMessage("§aResource pack loaded successfully!");
                break;

            case DECLINED:
                plugin.getResourcePackManager().setPlayerDeclinedResourcePack(player.getUniqueId());
                player.sendMessage("§cYou declined the resource pack. Some cosmetics may not display correctly.");
                break;

            case FAILED_DOWNLOAD:
                plugin.getResourcePackManager().resetPlayerPackStatus(player.getUniqueId());
                player.sendMessage("§cFailed to download the resource pack. Please try reconnecting.");

                // Try to apply the resource pack again after a delay
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        plugin.getResourcePackManager().applyResourcePack(player);
                    }
                }, 100L); // 5 seconds
                break;

            case ACCEPTED:
                player.sendMessage("§eLoading resource pack...");
                break;
        }
    }
}
