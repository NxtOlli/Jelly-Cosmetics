package com.jellymc.cosmetics.spigot.gui;

import com.jellymc.cosmetics.core.model.ChatColor;
import com.jellymc.cosmetics.spigot.JellyCosmeticsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatColorGUI implements Listener {
    private final JellyCosmeticsPlugin plugin;
    private final Player player;
    private final Map<Integer, String> slotToChatColorId = new HashMap<>();
    private Inventory inventory;

    private static final int ROWS = 3;
    private static final int SLOTS = ROWS * 9;

    public ChatColorGUI(JellyCosmeticsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        inventory = Bukkit.createInventory(null, SLOTS, "§8Chat Colors");

        // Load chat colors and populate inventory
        loadChatColors();

        // Open inventory for player
        player.openInventory(inventory);
    }

    private void loadChatColors() {
        // Clear existing items
        inventory.clear();
        slotToChatColorId.clear();

        // Get player's owned chat colors from database
        List<String> ownedChatColorIds = plugin.getDatabaseManager().getCosmeticRepository()
                .getPlayerCosmeticIdsByType(player.getUniqueId(), "CHAT_COLOR");

        // Get current chat color
        String currentChatColorId = plugin.getDatabaseManager().getCosmeticRepository()
                .getPlayerChatColor(player.getUniqueId());

        // Get all chat colors from config
        List<ChatColor> allChatColors = plugin.getConfigManager().getAllChatColors();

        int slot = 0;
        for (ChatColor chatColor : allChatColors) {
            boolean owned = ownedChatColorIds.contains(chatColor.getId());
            boolean selected = chatColor.getId().equals(currentChatColorId);

            // Create item representation
            ItemStack item = createChatColorItem(chatColor, owned, selected);

            // Add to inventory
            inventory.setItem(slot, item);

            if (owned) {
                slotToChatColorId.put(slot, chatColor.getId());
            }

            slot++;
            if (slot >= SLOTS) break;
        }

        // Add reset button
        ItemStack resetItem = new ItemStack(Material.BARRIER);
        ItemMeta meta = resetItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c§lReset Chat Color");
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to remove your chat color");
            meta.setLore(lore);
            resetItem.setItemMeta(meta);
        }
        inventory.setItem(SLOTS - 1, resetItem);
    }

    private ItemStack createChatColorItem(ChatColor chatColor, boolean owned, boolean selected) {
        ItemStack item = new ItemStack(owned ? Material.NAME_TAG : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Set display name
            String displayName = chatColor.getColorCode() + chatColor.getName();
            if (selected) {
                displayName = "§a§l[SELECTED] " + displayName;
            }
            meta.setDisplayName(displayName);

            // Add lore
            List<String> lore = new ArrayList<>();
            lore.add(chatColor.getColorCode() + "This is a preview of the chat color");
            lore.add("");

            if (owned) {
                lore.add("§7Status: §aUnlocked");
                lore.add("");
                if (selected) {
                    lore.add("§c§lClick to deselect");
                } else {
                    lore.add("§a§lClick to select");
                }
            } else {
                lore.add("§7Status: §cLocked");
                lore.add("§7Unlock this chat color from crates!");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != inventory) return;

        event.setCancelled(true);

        if (event.getClickedInventory() != inventory) return;

        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        // Handle reset button
        if (slot == SLOTS - 1) {
            plugin.getDatabaseManager().getCosmeticRepository().setPlayerChatColor(clicker.getUniqueId(), null);
            clicker.sendMessage("§aYour chat color has been reset!");

            // Notify other servers
            plugin.getSyncManager().notifyChatColorChanged(clicker.getUniqueId(), null);

            clicker.closeInventory();
            return;
        }

        // Handle chat color selection
        if (slotToChatColorId.containsKey(slot)) {
            String chatColorId = slotToChatColorId.get(slot);

            // Check if already selected
            String currentChatColorId = plugin.getDatabaseManager().getCosmeticRepository()
                    .getPlayerChatColor(clicker.getUniqueId());

            if (chatColorId.equals(currentChatColorId)) {
                // Deselect
                plugin.getDatabaseManager().getCosmeticRepository().setPlayerChatColor(clicker.getUniqueId(), null);
                clicker.sendMessage("§cChat color deselected!");

                // Notify other servers
                plugin.getSyncManager().notifyChatColorChanged(clicker.getUniqueId(), null);
            } else {
                // Select new chat color
                plugin.getDatabaseManager().getCosmeticRepository().setPlayerChatColor(clicker.getUniqueId(), chatColorId);

                // Get the chat color from config to show preview
                ChatColor chatColor = plugin.getConfigManager().getChatColorById(chatColorId);
                if (chatColor != null) {
                    clicker.sendMessage("§aChat color set to: " + chatColor.getColorCode() + "This color!");
                } else {
                    clicker.sendMessage("§aChat color selected!");
                }

                // Notify other servers
                plugin.getSyncManager().notifyChatColorChanged(clicker.getUniqueId(), chatColorId);
            }

            // Reload the inventory to reflect changes
            loadChatColors();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() == inventory) {
            // Unregister listeners when inventory is closed
            InventoryClickEvent.getHandlerList().unregister(this);
            InventoryCloseEvent.getHandlerList().unregister(this);
        }
    }
}
