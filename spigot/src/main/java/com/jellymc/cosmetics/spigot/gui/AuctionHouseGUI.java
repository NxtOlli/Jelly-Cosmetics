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
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.*;

public class AuctionHouseGUI implements Listener {
    private final JellyCosmeticsPlugin plugin;
    private final Player player;
    private final Map<Integer, Integer> slotToListingId = new HashMap<>();
    private Inventory inventory;
    private int currentPage = 0;
    private String currentFilter = null;

    private static final int ROWS = 6;
    private static final int SLOTS = ROWS * 9;
    private static final int LISTING_SLOTS = SLOTS - 18; // Reserve bottom two rows for navigation
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm");

    public AuctionHouseGUI(JellyCosmeticsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        inventory = Bukkit.createInventory(null, SLOTS, "§8Cosmetic Auction House");

        // Load listings and populate inventory
        loadListings();

        // Open inventory for player
        player.openInventory(inventory);
    }

    private void loadListings() {
        // Clear existing items
        inventory.clear();
        slotToListingId.clear();

        // Get active auction listings from database
        List<AuctionListing> listings = plugin.getDatabaseManager().getAuctionRepository().getActiveListings(currentFilter);

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

        // Add action buttons
        addActionButtons();
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
            lore.add("§7Seller: §f" + Bukkit.getOfflinePlayer(listing.getSellerUuid()).getName());
            lore.add("§7Listed: §f" + DATE_FORMAT.format(listing.getListedAt()));
            lore.add("§7Expires: §f" + DATE_FORMAT.format(listing.getExpiresAt()));
            lore.add("");
            lore.add("§a§lClick to purchase");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        // TODO: Apply custom model data based on textureId
        // if (meta != null && !cosmetic.getTextureId().isEmpty()) {
        //     meta.setCustomModelData(Integer.parseInt(cosmetic.getTextureId()));
        //     item.setItemMeta(meta);
        // }

        return item;
    }

    private void addNavigationItems(int totalPages) {
        // Bottom row starts at slot SLOTS - 9

        // Filter buttons
        ItemStack allFilter = createFilterButton(null, "All Cosmetics");
        ItemStack toolsFilter = createFilterButton("TOOL", "Tools");
        ItemStack weaponsFilter = createFilterButton("WEAPON", "Weapons");
        ItemStack chatColorFilter = createFilterButton("CHAT_COLOR", "Chat Colors");
        ItemStack namePaintFilter = createFilterButton("NAME_PAINT", "Name Paints");

        inventory.setItem(SLOTS - 18, allFilter);
        inventory.setItem(SLOTS - 17, toolsFilter);
        inventory.setItem(SLOTS - 16, weaponsFilter);
        inventory.setItem(SLOTS - 15, chatColorFilter);
        inventory.setItem(SLOTS - 14, namePaintFilter);

        // Pagination
        if (currentPage > 0) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta meta = prevPage.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§a§lPrevious Page");
                prevPage.setItemMeta(meta);
            }
            inventory.setItem(SLOTS - 3, prevPage);
        }

        if (currentPage < totalPages - 1) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta meta = nextPage.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§a§lNext Page");
                nextPage.setItemMeta(meta);
            }
            inventory.setItem(SLOTS - 1, nextPage);
        }

        // Page indicator
        ItemStack pageIndicator = new ItemStack(Material.PAPER);
        ItemMeta meta = pageIndicator.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§7Page " + (currentPage + 1) + " of " + Math.max(1, totalPages));
            pageIndicator.setItemMeta(meta);
        }
        inventory.setItem(SLOTS - 2, pageIndicator);
    }

    private void addActionButtons() {
        // My listings button
        ItemStack myListings = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) myListings.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName("§e§lMy Listings");
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to view your active listings");
            skullMeta.setLore(lore);
            myListings.setItemMeta(skullMeta);
        }
        inventory.setItem(SLOTS - 9, myListings);

        // Sell cosmetic button
        ItemStack sellItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta sellMeta = sellItem.getItemMeta();
        if (sellMeta != null) {
            sellMeta.setDisplayName("§6§lSell Cosmetic");
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to list one of your cosmetics");
            lore.add("§7for sale on the auction house");
            sellMeta.setLore(lore);
            sellItem.setItemMeta(sellMeta);
        }
        inventory.setItem(SLOTS - 5, sellItem);
    }

    private ItemStack createFilterButton(String filter, String name) {
        Material material;
        boolean selected = (Objects.equals(currentFilter, filter));

        if (filter == null) {
            material = Material.CHEST;
        } else {
            switch (filter) {
                case "TOOL":
                    material = Material.DIAMOND_PICKAXE;
                    break;
                case "WEAPON":
                    material = Material.DIAMOND_SWORD;
                    break;
                case "CHAT_COLOR":
                    material = Material.NAME_TAG;
                    break;
                case "NAME_PAINT":
                    material = Material.PAINTING;
                    break;
                default:
                    material = Material.CHEST;
            }
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((selected ? "§a§l" : "§7") + name);
            List<String> lore = new ArrayList<>();
            lore.add(selected ? "§aCurrently selected" : "§7Click to select");
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

        // Handle navigation row
        if (slot >= SLOTS - 18) {
            handleNavigationClick(clicker, slot);
            return;
        }

        // Handle listing click
        if (slotToListingId.containsKey(slot)) {
            int listingId = slotToListingId.get(slot);
            handleListingClick(clicker, listingId);
        }
    }

    private void handleNavigationClick(Player clicker, int slot) {
        // Filter buttons
        if (slot == SLOTS - 18) {
            // All cosmetics
            currentFilter = null;
            loadListings();
        } else if (slot == SLOTS - 17) {
            // Tools filter
            currentFilter = "TOOL";
            loadListings();
        } else if (slot == SLOTS - 16) {
            // Weapons filter
            currentFilter = "WEAPON";
            loadListings();
        } else if (slot == SLOTS - 15) {
            // Chat colors filter
            currentFilter = "CHAT_COLOR";
            loadListings();
        } else if (slot == SLOTS - 14) {
            // Name paints filter
            currentFilter = "NAME_PAINT";
            loadListings();
        }

        // Pagination
        else if (slot == SLOTS - 3) {
            // Previous page
            if (currentPage > 0) {
                currentPage--;
                loadListings();
            }
        } else if (slot == SLOTS - 1) {
            // Next page
            currentPage++;
            loadListings();
        }

        // Action buttons
        else if (slot == SLOTS - 9) {
            // My listings
            clicker.closeInventory();
            new MyListingsGUI(plugin, clicker).open();
        } else if (slot == SLOTS - 5) {
            // Sell cosmetic
            clicker.closeInventory();
            new SellCosmeticGUI(plugin, clicker).open();
        }
    }

    private void handleListingClick(Player clicker, int listingId) {
        // Get listing details
        AuctionListing listing = plugin.getDatabaseManager().getAuctionRepository().getListing(listingId);

        if (listing == null || !listing.getStatus().equals("ACTIVE")) {
            clicker.sendMessage("§cThis listing is no longer available.");
            loadListings(); // Refresh to remove the listing
            return;
        }

        // Check if player is trying to buy their own listing
        if (listing.getSellerUuid().equals(clicker.getUniqueId())) {
            clicker.sendMessage("§cYou cannot buy your own listing.");
            return;
        }

        // Check if player already owns this cosmetic
        if (plugin.getDatabaseManager().getCosmeticRepository().playerOwnsCosmetic(
                clicker.getUniqueId(), listing.getCosmeticId())) {
            clicker.sendMessage("§cYou already own this cosmetic.");
            return;
        }

        // Open purchase confirmation GUI
        clicker.closeInventory();
        new PurchaseConfirmationGUI(plugin, clicker, listing).open();
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
