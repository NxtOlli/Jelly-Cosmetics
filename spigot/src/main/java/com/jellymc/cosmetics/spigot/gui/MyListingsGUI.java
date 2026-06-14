package com.jellymc.cosmetics.spigot.gui;

import com.jellymc.cosmetics.core.model.AuctionListing;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyListingsGUI implements Listener {
    private final JellyCosmeticsPlugin plugin;
    private final Player player;
    private final Map<Integer, Integer> slotToListingId = new HashMap<>();
    private Inventory inventory;
    private int currentPage = 0;

    private static final int ROWS = 6;
    private static final int SLOTS = ROWS * 9;
    private static final int LISTING_SLOTS = SLOTS - 9; // Reserve bottom row for navigation
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm");

    public MyListingsGUI(JellyCosmeticsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        inventory = Bukkit.createInventory(null, SLOTS, "§8My Auction Listings");

        // Load listings and populate inventory
        loadListings();

        // Open inventory for player
        player.openInventory(inventory);
    }

    private void loadListings() {
        // Clear existing items
        inventory.clear();
        slotToListingId.clear();

        // Get player's active auction listings from database
        List<AuctionListing> listings = plugin.getDatabaseManager().getAuctionRepository()
                .getPlayerListings(player.getUniqueId());

        // Calculate pagination
        int totalPages = (int) Math.ceil((double) listings.size() / LISTING_SLOTS);
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }

        // Add listings to inventory
        int startIndex = currentPage * LISTING_SLOTS;
        int endIndex = Math.min(startIndex + LISTING_SLOTS, listings.size());

        for (int i = startIndex; i < endIndex; i++) {
            AuctionListing listing = listings.get(i);
            int slot = i - startIndex;

            // Get cosmetic info
            Cosmetic cosmetic = plugin.getConfigManager().getCosmeticById(listing.getCosmeticId());
            if (cosmetic == null) continue;

            // Create item representation
            ItemStack item = createListingItem(listing, cosmetic);

            // Add to inventory
            inventory.setItem(slot, item);
            slotToListingId.put(slot, listing.getId());
        }

        // Add navigation items
        addNavigationItems(totalPages);
    }

    private ItemStack createListingItem(AuctionListing listing, Cosmetic cosmetic) {
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

            // Add listing info to lore
            List<String> lore = new ArrayList<>();
            lore.add("§7Type: §f" + cosmetic.type().name());
            lore.add("§7Rarity: " + cosmetic.getRarityColor() + cosmetic.rarity());
            lore.add("");
            lore.add("§6Price: §e" + listing.getPrice() + " coins");
            lore.add("§7Listed: §f" + DATE_FORMAT.format(listing.getListedAt()));
            lore.add("§7Expires: §f" + DATE_FORMAT.format(listing.getExpiresAt()));
            lore.add("§7Remaining: §f" + listing.getRemainingTimeFormatted());
            lore.add("");
            lore.add("§c§lClick to cancel listing");

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
                loadListings();
            }
            return;
        }

        if (slot == SLOTS - 4) {
            // Next page
            currentPage++;
            loadListings();
            return;
        }

        // Handle listing click (cancel listing)
        if (slotToListingId.containsKey(slot)) {
            int listingId = slotToListingId.get(slot);

            // Confirm cancellation
            clicker.closeInventory();
            new CancelListingConfirmationGUI(plugin, clicker, listingId).open();
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
