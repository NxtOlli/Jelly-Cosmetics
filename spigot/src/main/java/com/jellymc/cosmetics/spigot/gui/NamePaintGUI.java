package com.jellymc.cosmetics.spigot.gui;

import com.jellymc.cosmetics.core.model.NamePaint;
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

public class NamePaintGUI implements Listener {
    private final JellyCosmeticsPlugin plugin;
    private final Player player;
    private final Map<Integer, String> slotToNamePaintId = new HashMap<>();
    private Inventory inventory;

    private static final int ROWS = 3;
    private static final int SLOTS = ROWS * 9;

    public NamePaintGUI(JellyCosmeticsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        inventory = Bukkit.createInventory(null, SLOTS, "§8Name Paints");

        // Load name paints and populate inventory
        loadNamePaints();

        // Open inventory for player
        player.openInventory(inventory);
    }

    private void loadNamePaints() {
        // Clear existing items
        inventory.clear();
        slotToNamePaintId.clear();

        // Get player's owned name paints from database
        List<String> ownedNamePaintIds = plugin.getDatabaseManager().getCosmeticRepository()
                .getPlayerCosmeticIdsByType(player.getUniqueId(), "NAME_PAINT");

        // Get current name paint
        String currentNamePaintId = plugin.getDatabaseManager().getCosmeticRepository()
                .getPlayerNamePaint(player.getUniqueId());

        // Get all name paints from config
        List<NamePaint> allNamePaints = plugin.getConfigManager().getAllNamePaints();

        int slot = 0;
        for (NamePaint namePaint : allNamePaints) {
            boolean owned = ownedNamePaintIds.contains(namePaint.getId());
            boolean selected = namePaint.getId().equals(currentNamePaintId);

            // Create item representation
            ItemStack item = createNamePaintItem(namePaint, owned, selected);

            // Add to inventory
            inventory.setItem(slot, item);

            if (owned) {
                slotToNamePaintId.put(slot, namePaint.getId());
            }

            slot++;
            if (slot >= SLOTS) break;
        }

        // Add reset button
        ItemStack resetItem = new ItemStack(Material.BARRIER);
        ItemMeta meta = resetItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c§lReset Name Paint");
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to remove your name paint");
            meta.setLore(lore);
            resetItem.setItemMeta(meta);
        }
        inventory.setItem(SLOTS - 1, resetItem);
    }

    private ItemStack createNamePaintItem(NamePaint namePaint, boolean owned, boolean selected) {
        ItemStack item = new ItemStack(owned ? Material.PAINTING : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Set display name
            String displayName = "§r" + namePaint.getName();
            if (selected) {
                displayName = "§a§l[SELECTED] " + displayName;
            }
            meta.setDisplayName(displayName);

            // Add lore
            List<String> lore = new ArrayList<>();
            lore.add("§7Preview: " + namePaint.formatName(player.getName()));
            lore.add("");

            if (namePaint.isAnimated()) {
                lore.add("§d§lANIMATED");
            }

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
                lore.add("§7Unlock this name paint from crates!");
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
            plugin.getDatabaseManager().getCosmeticRepository().setPlayerNamePaint(clicker.getUniqueId(), null);
            clicker.sendMessage("§aYour name paint has been reset!");

            // Reset display name
            clicker.setDisplayName(clicker.getName());

            // Notify other servers
            plugin.getSyncManager().notifyNamePaintChanged(clicker.getUniqueId(), null);

            clicker.closeInventory();
            return;
        }

        // Handle name paint selection
        if (slotToNamePaintId.containsKey(slot)) {
            String namePaintId = slotToNamePaintId.get(slot);

            // Check if already selected
            String currentNamePaintId = plugin.getDatabaseManager().getCosmeticRepository()
                    .getPlayerNamePaint(clicker.getUniqueId());

            if (namePaintId.equals(currentNamePaintId)) {
                // Deselect
                plugin.getDatabaseManager().getCosmeticRepository().setPlayerNamePaint(clicker.getUniqueId(), null);
                clicker.sendMessage("§cName paint deselected!");

                // Reset display name
                clicker.setDisplayName(clicker.getName());

                // Notify other servers
                plugin.getSyncManager().notifyNamePaintChanged(clicker.getUniqueId(), null);
            } else {
                // Select new name paint
                plugin.getDatabaseManager().getCosmeticRepository().setPlayerNamePaint(clicker.getUniqueId(), namePaintId);

                // Get the name paint from config to show preview
                NamePaint namePaint = plugin.getConfigManager().getNamePaintById(namePaintId);
                if (namePaint != null) {
                    String formattedName = namePaint.formatName(clicker.getName());
                    clicker.sendMessage("§aName paint set to: " + formattedName);

                    // Update display name
                    clicker.setDisplayName(formattedName);
                } else {
                    clicker.sendMessage("§aName paint selected!");
                }

                // Notify other servers
                plugin.getSyncManager().notifyNamePaintChanged(clicker.getUniqueId(), namePaintId);
            }

            // Reload the inventory to reflect changes
            loadNamePaints();
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
