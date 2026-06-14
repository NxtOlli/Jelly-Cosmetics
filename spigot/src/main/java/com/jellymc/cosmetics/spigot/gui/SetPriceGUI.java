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
import java.util.List;

public class SetPriceGUI implements Listener {
    private final JellyCosmeticsPlugin plugin;
    private final Player player;
    private final String cosmeticId;
    private Inventory inventory;
    private double currentPrice;

    // Price adjustment buttons
    private static final int MINUS_100_SLOT = 10;
    private static final int MINUS_10_SLOT = 11;
    private static final int MINUS_1_SLOT = 12;
    private static final int PRICE_DISPLAY_SLOT = 13;
    private static final int PLUS_1_SLOT = 14;
    private static final int PLUS_10_SLOT = 15;
    private static final int PLUS_100_SLOT = 16;

    // Action buttons
    private static final int CONFIRM_SLOT = 22;
    private static final int CANCEL_SLOT = 26;

    // Price limits
    private final double minPrice;
    private final double maxPrice;

    public SetPriceGUI(JellyCosmeticsPlugin plugin, Player player, String cosmeticId) {
        this.plugin = plugin;
        this.player = player;
        this.cosmeticId = cosmeticId;

        // Get price limits from config
        this.minPrice = plugin.getConfig().getDouble("auction.min_price", 5.0);
        this.maxPrice = plugin.getConfig().getDouble("auction.max_price", 1000000.0);

        // Start with minimum price
        this.currentPrice = minPrice;

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 27, "§8Set Listing Price");

        // Get cosmetic info
        Cosmetic cosmetic = plugin.getConfigManager().getCosmeticById(cosmeticId);
        if (cosmetic == null) {
            player.sendMessage("§cCannot find cosmetic information.");
            player.closeInventory();
            return;
        }

        // Add cosmetic item
        Material material;
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

        ItemStack cosmeticItem = new ItemStack(material);
        ItemMeta cosmeticMeta = cosmeticItem.getItemMeta();
        if (cosmeticMeta != null) {
            cosmeticMeta.setDisplayName("§r" + cosmetic.name());
            List<String> lore = new ArrayList<>();
            lore.add("§7Type: §f" + cosmetic.type().name());
            lore.add("§7Rarity: " + cosmetic.getRarityColor() + cosmetic.rarity());
            cosmeticMeta.setLore(lore);
            cosmeticItem.setItemMeta(cosmeticMeta);
        }
        inventory.setItem(4, cosmeticItem);

        // Add price adjustment buttons
        inventory.setItem(MINUS_100_SLOT, createButton(Material.RED_CONCRETE, "§c-100", "§7Decrease price by 100"));
        inventory.setItem(MINUS_10_SLOT, createButton(Material.RED_CONCRETE, "§c-10", "§7Decrease price by 10"));
        inventory.setItem(MINUS_1_SLOT, createButton(Material.RED_CONCRETE, "§c-1", "§7Decrease price by 1"));

        updatePriceDisplay();

        inventory.setItem(PLUS_1_SLOT, createButton(Material.LIME_CONCRETE, "§a+1", "§7Increase price by 1"));
        inventory.setItem(PLUS_10_SLOT, createButton(Material.LIME_CONCRETE, "§a+10", "§7Increase price by 10"));
        inventory.setItem(PLUS_100_SLOT, createButton(Material.LIME_CONCRETE, "§a+100", "§7Increase price by 100"));

        // Add action buttons
        inventory.setItem(CONFIRM_SLOT, createButton(Material.EMERALD_BLOCK, "§a§lConfirm Listing",
                "§7List this cosmetic for §e" + currentPrice + " coins",
                "§7Listing fee: §e" + getListingFee() + " coins",
                "§7Duration: §f" + getListingDuration() + " days"));

        inventory.setItem(CANCEL_SLOT, createButton(Material.BARRIER, "§c§lCancel", "§7Return to auction house"));

        // Open inventory
        player.openInventory(inventory);
    }

    private ItemStack createButton(Material material, String name, String... lore) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);

            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(line);
                }
                meta.setLore(loreList);
            }

            button.setItemMeta(meta);
        }
        return button;
    }

    private void updatePriceDisplay() {
        // Format price to 2 decimal places
        String formattedPrice = String.format("%.2f", currentPrice);

        ItemStack priceDisplay = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = priceDisplay.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lPrice: §e" + formattedPrice + " coins");
            List<String> lore = new ArrayList<>();
            lore.add("§7Click the + and - buttons");
            lore.add("§7to adjust the price");
            lore.add("");
            lore.add("§7Min price: §e" + minPrice + " coins");
            lore.add("§7Max price: §e" + maxPrice + " coins");
            meta.setLore(lore);
            priceDisplay.setItemMeta(meta);
        }

        inventory.setItem(PRICE_DISPLAY_SLOT, priceDisplay);

        // Update confirm button
        ItemStack confirmButton = inventory.getItem(CONFIRM_SLOT);
        if (confirmButton != null) {
            ItemMeta confirmMeta = confirmButton.getItemMeta();
            if (confirmMeta != null) {
                List<String> lore = new ArrayList<>();
                lore.add("§7List this cosmetic for §e" + formattedPrice + " coins");
                lore.add("§7Listing fee: §e" + getListingFee() + " coins");
                lore.add("§7Duration: §f" + getListingDuration() + " days");
                confirmMeta.setLore(lore);
                confirmButton.setItemMeta(confirmMeta);
            }
        }
    }

    private double getListingFee() {
        // Calculate listing fee based on config
        return plugin.getConfig().getDouble("auction.listing_fee", 10.0);
    }

    private int getListingDuration() {
        // Get listing duration from config
        return plugin.getConfig().getInt("auction.listing_duration", 7);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != inventory) return;

        event.setCancelled(true);

        if (event.getClickedInventory() != inventory) return;

        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        // Handle price adjustment buttons
        if (slot == MINUS_100_SLOT) {
            adjustPrice(-100);
        } else if (slot == MINUS_10_SLOT) {
            adjustPrice(-10);
        } else if (slot == MINUS_1_SLOT) {
            adjustPrice(-1);
        } else if (slot == PLUS_1_SLOT) {
            adjustPrice(1);
        } else if (slot == PLUS_10_SLOT) {
            adjustPrice(10);
        } else if (slot == PLUS_100_SLOT) {
            adjustPrice(100);
        }

        // Handle action buttons
        else if (slot == CONFIRM_SLOT) {
            // Check if player has enough money for the listing fee
            double listingFee = getListingFee();
            double balance = getPlayerBalance(clicker);

            if (balance < listingFee) {
                clicker.sendMessage("§cYou don't have enough coins to pay the listing fee!");
                clicker.closeInventory();
                return;
            }

            // Create the listing
            int listingId = plugin.getDatabaseManager().getAuctionRepository().createListing(
                    clicker.getUniqueId(),
                    cosmeticId,
                    currentPrice,
                    getListingDuration() * 24 // Convert days to hours
            );

            if (listingId > 0) {
                // Deduct listing fee
                withdrawPlayerBalance(clicker, listingFee);

                // Remove cosmetic from player's inventory
                plugin.getDatabaseManager().getCosmeticRepository().removeCosmetic(clicker.getUniqueId(), cosmeticId);

                // Get cosmetic info
                Cosmetic cosmetic = plugin.getConfigManager().getCosmeticById(cosmeticId);
                String cosmeticName = cosmetic != null ? cosmetic.name() : "cosmetic";

                // Notify the player
                clicker.sendMessage("§aYou have listed §e" + cosmeticName + " §afor §e" + currentPrice + " coins§a!");

                // Notify other servers
                plugin.getSyncManager().sendSyncMessage(
                        com.jellymc.cosmetics.core.sync.SyncMessage.auctionListingCreated(
                                clicker.getUniqueId(), listingId));
            } else {
                clicker.sendMessage("§cFailed to create listing. Please try again later.");
            }

            // Close inventory and return to Auction House
            clicker.closeInventory();
            new AuctionHouseGUI(plugin, clicker).open();
        } else if (slot == CANCEL_SLOT) {
            // Return to Auction House without listing
            clicker.closeInventory();
            new AuctionHouseGUI(plugin, clicker).open();
        }
    }

    private void adjustPrice(double amount) {
        // Adjust price within limits
        double newPrice = currentPrice + amount;

        // Round to 2 decimal places
        newPrice = Math.round(newPrice * 100) / 100.0;

        if (newPrice < minPrice) {
            newPrice = minPrice;
        } else if (newPrice > maxPrice) {
            newPrice = maxPrice;
        }

        currentPrice = newPrice;
        updatePriceDisplay();
    }

    /**
     * Gets the player's current balance
     * This is a placeholder method - you would integrate with your economy plugin
     *
     * @param player The player to check
     * @return The player's balance
     */
    private double getPlayerBalance(Player player) {
        // TODO: Integrate with your economy plugin
        // Example with Vault:
        // return plugin.getEconomy().getBalance(player);

        // For now, return a large amount for testing
        return 10000.0;
    }

    /**
     * Withdraws money from the player's balance
     * This is a placeholder method - you would integrate with your economy plugin
     *
     * @param player The player to withdraw from
     * @param amount The amount to withdraw
     */
    private void withdrawPlayerBalance(Player player, double amount) {
        // TODO: Integrate with your economy plugin
        // Example with Vault:
        // plugin.getEconomy().withdrawPlayer(player, amount);

        // For now, just log the transaction
        plugin.getLogger().info("Withdrew " + amount + " coins from " + player.getName());
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
