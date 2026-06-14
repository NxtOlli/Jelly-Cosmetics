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

import java.util.ArrayList;
import java.util.List;

public class PurchaseConfirmationGUI implements Listener {
    private final JellyCosmeticsPlugin plugin;
    private final Player player;
    private final AuctionListing listing;
    private Inventory inventory;

    private static final int CONFIRM_SLOT = 11;
    private static final int CANCEL_SLOT = 15;

    public PurchaseConfirmationGUI(JellyCosmeticsPlugin plugin, Player player, AuctionListing listing) {
        this.plugin = plugin;
        this.player = player;
        this.listing = listing;

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 27, "§8Confirm Purchase");

        // Get cosmetic info
        Cosmetic cosmetic = plugin.getConfigManager().getCosmeticById(listing.getCosmeticId());
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
            lore.add("");
            lore.add("§6Price: §e" + listing.getPrice() + " coins");
            lore.add("§7Seller: §f" + Bukkit.getOfflinePlayer(listing.getSellerUuid()).getName());
            cosmeticMeta.setLore(lore);
            cosmeticItem.setItemMeta(cosmeticMeta);
        }
        inventory.setItem(13, cosmeticItem);

        // Add confirm button
        ItemStack confirmItem = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName("§a§lConfirm Purchase");
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to purchase this cosmetic");
            lore.add("§7for §e" + listing.getPrice() + " coins");
            confirmMeta.setLore(lore);
            confirmItem.setItemMeta(confirmMeta);
        }
        inventory.setItem(CONFIRM_SLOT, confirmItem);

        // Add cancel button
        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName("§c§lCancel");
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to cancel this purchase");
            cancelMeta.setLore(lore);
            cancelItem.setItemMeta(cancelMeta);
        }
        inventory.setItem(CANCEL_SLOT, cancelItem);

        // Open inventory
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != inventory) return;

        event.setCancelled(true);

        if (event.getClickedInventory() != inventory) return;

        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == CONFIRM_SLOT) {
            // Check if player has enough money
            double balance = getPlayerBalance(clicker);

            if (balance < listing.getPrice()) {
                clicker.sendMessage("§cYou don't have enough coins to purchase this cosmetic!");
                clicker.closeInventory();
                return;
            }

            // Process the purchase
            boolean success = plugin.getDatabaseManager().getAuctionRepository().purchaseListing(listing.getId(), clicker.getUniqueId());

            if (success) {
                // Deduct coins from player
                withdrawPlayerBalance(clicker, listing.getPrice());

                // Get cosmetic info
                Cosmetic cosmetic = plugin.getConfigManager().getCosmeticById(listing.getCosmeticId());
                String cosmeticName = cosmetic != null ? cosmetic.name() : "cosmetic";

                // Notify the player
                clicker.sendMessage("§aYou successfully purchased §e" + cosmeticName + " §afor §e" + listing.getPrice() + " coins§a!");

                // Notify other servers
                plugin.getSyncManager().sendSyncMessage(
                        com.jellymc.cosmetics.core.sync.SyncMessage.auctionListingPurchased(
                                clicker.getUniqueId(), listing.getId()));

                // Notify the seller if they're online
                Player seller = Bukkit.getPlayer(listing.getSellerUuid());
                if (seller != null) {
                    seller.sendMessage("§aYour listing for §e" + cosmeticName + " §ahas been sold for §e" + listing.getPrice() + " coins§a!");
                }
            } else {
                clicker.sendMessage("§cFailed to purchase the cosmetic. It may have already been sold or expired.");
            }

            // Close inventory and return to Auction House
            clicker.closeInventory();
            new AuctionHouseGUI(plugin, clicker).open();
        } else if (slot == CANCEL_SLOT) {
            // Return to Auction House without purchasing
            clicker.closeInventory();
            new AuctionHouseGUI(plugin, clicker).open();
        }
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
