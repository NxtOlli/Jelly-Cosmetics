package com.jellymc.cosmetics.spigot.gui;

import com.jellymc.cosmetics.core.model.Cosmetic;
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

public class SellCosmeticGUI implements Listener {
    private final JellyCosmeticsPlugin plugin;
    private final Player player;
    private final Map<Integer, String> slotToCosmeticId = new HashMap<>();
    private Inventory inventory;
    private int currentPage = 0;

    private static final int ROWS = 6;
    private static final int SLOTS = ROWS * 9;
    private static final int COSMETIC_SLOTS = SLOTS - 9; // Reserve bottom row for navigation

    public SellCosmeticGUI(JellyCosmeticsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        inventory = Bukkit.createInventory(null, SLOTS, "§8Select Cosmetic to Sell");

        // Load cosmetics and populate inventory
        loadCosmetics();

        // Open inventory for player
        player.openInventory(inventory);
    }

    private void loadCosmetics() {
        // Clear existing items
        inventory.clear();
        slotToCosmeticId.clear();

        // Get player's cosmetics from database
        List<String> ownedCosmeticIds = plugin.getDatabaseManager().getCosmeticRepository()
                .getPlayerCosmeticIds(player.getUniqueId());

        // Get all cosmetics from config
        List<Cosmetic> allCosmetics = plugin.getConfigManager().getAllCosmetics();

        // Filter tradeable cosmetics
        List<Cosmetic> tradeableCosmetics = new ArrayList<>();
        for (Cosmetic cosmetic : allCosmetics) {
            if (ownedCosmeticIds.contains(cosmetic.id()) && cosmetic.tradeable()) {
                tradeableCosmetics.add(cosmetic);
            }
        }

        // Calculate pagination
        int totalPages = (int) Math.ceil((double) tradeableCosmetics.size() / COSMETIC_SLOTS);
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }

        // Add cosmetics to inventory
        int startIndex = currentPage * COSMETIC_SLOTS;
        int endIndex = Math.min(startIndex + COSMETIC_SLOTS, tradeableCosmetics.size());

        for (int i = startIndex; i < endIndex; i++) {
            Cosmetic cosmetic = tradeableCosmetics.get(i);
            int slot = i - startIndex;

            // Create item representation
            ItemStack item = createCosmeticItem(cosmetic);

            // Add to inventory
            inventory.setItem(slot, item);
            slotToCosmeticId.put(slot, cosmetic.id());
        }

        // Add navigation items
        addNavigationItems(totalPages);
    }

    private ItemStack createCosmeticItem(Cosmetic cosmetic) {
        Material material;

        // Choose appropriate material based on cosmetic type
        switch (cosmetic.type()) {
            case PICKAXE:
                material = Material.DIAMOND_PICKAXE;
                break;
            case AXE:
                material = Material.DIAMOND_AXE;
                break;
            case SHOVEL:
                material = Material.DIAMOND_SHOVEL;
                break;
            case HOE:
                material = Material.DIAMOND_HOE;
                break;
            case SWORD:
                material = Material.DIAMOND_SWORD;
                break;
            case BOW:
                material = Material.BOW;
                break;
            case TRIDENT:
                material = Material.TRIDENT;
                break;
            case CHAT_COLOR:
                material = Material.NAME_TAG;
                break;
            case NAME_PAINT:
                material = Material.PAINTING;
                break;
            case PARTICLE:
                material = Material.BLAZE_POWDER;
                break;
            default:
                material = Material.PAPER;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Set display name
            meta.setDisplayName("§r" + cosmetic.name());

            // Add cosmetic info to lore
            List<String> lore = new ArrayList<>(cosmetic.description());
            lore.add("");
            lore.add("§7Type: §f" + cosmetic.type().name());
            lore.add("§7Rarity: " + cosmetic.getRarityColor() + cosmetic.rarity());
            lore.add("");
            lore.add("§a§lClick to sell this cosmetic");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private void addNavigationItems(int totalPages) {
        // Bottom row starts at slot SLOTS - 9

        // Back button
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c§lBack to Auction House");
            backButton.setItemMeta(backMeta);
        }
        inventory.setItem(SLOTS - 9, backButton);

        // Pagination
        if (currentPage > 0) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta meta = prevPage.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§a§lPrevious Page");
                prevPage.setItemMeta(meta);
            }
            inventory.setItem(SLOTS - 6, prevPage);
        }

        if (currentPage < totalPages - 1) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta meta = nextPage.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§a§lNext Page");
                nextPage.setItemMeta(meta);
            }
            inventory.setItem(SLOTS - 4, nextPage);
        }

        // Page indicator
        ItemStack pageIndicator = new ItemStack(Material.PAPER);
        ItemMeta meta = pageIndicator.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§7Page " + (currentPage + 1) + " of " + Math.max(1, totalPages));
            pageIndicator.setItemMeta(meta);
        }
        inventory.setItem(SLOTS - 5, pageIndicator);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != inventory) return;

        event.setCancelled(true);

        if (event.getClickedInventory() != inventory) return;

        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        // Handle back button
        if (slot == SLOTS - 9) {
            clicker.closeInventory();
            new AuctionHouseGUI(plugin, clicker).open();
            return;
        }

        // Handle pagination
        if (slot == SLOTS - 6) {
            // Previous page
            if (currentPage > 0) {
                currentPage--;
                loadCosmetics();
            }
            return;
        }

        if (slot == SLOTS - 4) {
            // Next page
            currentPage++;
            loadCosmetics();
            return;
        }

        // Handle cosmetic click (sell cosmetic)
        if (slotToCosmeticId.containsKey(slot)) {
            String cosmeticId = slotToCosmeticId.get(slot);
            Cosmetic cosmetic = plugin.getConfigManager().getCosmeticById(cosmeticId);

            if (cosmetic != null) {
                // Open price setting GUI
                clicker.closeInventory();
                new SetPriceGUI(plugin, clicker, cosmeticId).open();
            }
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
