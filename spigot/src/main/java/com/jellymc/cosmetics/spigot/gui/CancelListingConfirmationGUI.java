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

public class CancelListingConfirmationGUI implements Listener {
    private final JellyCosmeticsPlugin plugin;
    private final Player player;
    private final int listingId;
    private Inventory inventory;

    private static final int CONFIRM_SLOT = 11;
    private static final int CANCEL_SLOT = 15;

    public CancelListingConfirmationGUI(JellyCosmeticsPlugin plugin, Player player, int listingId) {
        this.plugin = plugin;
        this.player = player;
        this.listingId = listingId;

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 27, "§8Cancel Listing?");

        // Get listing details
        AuctionListing listing = plugin.getDatabaseManager().getAuctionRepository().getListing(listingId);

        if (listing == null || !listing.getStatus().equals("ACTIVE")) {
            player.sendMessage("§cThis listing is no longer available.");
            player.closeInventory();
            return;
        }

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
            cosmeticMeta.setLore(lore);
            cosmeticItem.setItemMeta(cosmeticMeta);
        }
        inventory.setItem(13, cosmeticItem);

        // Add confirm button
        ItemStack confirmItem = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName("§a§lConfirm Cancellation");
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to cancel this listing");
            lore.add("§7and return the cosmetic to your inventory");
            confirmMeta.setLore(lore);
            confirmItem.setItemMeta(confirmMeta);
        }
        inventory.setItem(CONFIRM_SLOT, confirmItem);

        // Add cancel button
        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName("§c§lKeep Listing");
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to keep this listing active");
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
            // Cancel the listing
            boolean success = plugin.getDatabaseManager().getAuctionRepository().cancelListing(listingId, clicker.getUniqueId());

            if (success) {
                // Get the cosmetic ID
                AuctionListing listing = plugin.getDatabaseManager().getAuctionRepository().getListing(listingId);
                if (listing != null) {
                    String cosmeticId = listing.getCosmeticId();

                    // Return the cosmetic to the player
                    plugin.getDatabaseManager().getCosmeticRepository().giveCosmetic(clicker.getUniqueId(), cosmeticId);

                    // Notify the player
                    clicker.sendMessage("§aListing cancelled successfully! The cosmetic has been returned to your inventory.");

                    // Notify other servers
                    plugin.getSyncManager().sendSyncMessage(
                            com.jellymc.cosmetics.core.sync.SyncMessage.auctionListingCancelled(
                                    clicker.getUniqueId(), listingId));
                } else {
                    clicker.sendMessage("§aListing cancelled successfully!");
                }
            } else {
                clicker.sendMessage("§cFailed to cancel listing. It may have already been sold or expired.");
            }

            // Close inventory and return to My Listings
            clicker.closeInventory();
            new MyListingsGUI(plugin, clicker).open();
        } else if (slot == CANCEL_SLOT) {
            // Return to My Listings without cancelling
            clicker.closeInventory();
            new MyListingsGUI(plugin, clicker).open();
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
