package com.jellymc.cosmetics.spigot.gui;

import com.jellymc.cosmetics.core.model.Cosmetic;
import com.jellymc.cosmetics.core.model.CosmeticType;
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

import java.util.*;

public class CosmeticInventoryGUI implements Listener {

    private final JellyCosmeticsPlugin plugin;
    private final Player player;
    private final UUID targetUuid;
    private final boolean viewOnly;
    private final Map<Integer, String> slotToCosmeticId = new HashMap<>();
    private Inventory inventory;
    private int currentPage = 0;
    private CosmeticType currentFilter = null;

    private static final int ROWS = 6;
    private static final int SLOTS = ROWS * 9;
    private static final int COSMETIC_SLOTS = SLOTS - 9; // Reserve bottom row for navigation

    public CosmeticInventoryGUI(JellyCosmeticsPlugin plugin, Player player) {
        this(plugin, player, player.getUniqueId(), false);
    }

    public CosmeticInventoryGUI(JellyCosmeticsPlugin plugin, Player player, UUID targetUuid, boolean viewOnly) {
        this.plugin = plugin;
        this.player = player;
        this.targetUuid = targetUuid;
        this.viewOnly = viewOnly;

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        String title = viewOnly ?
                "§8Viewing " + Bukkit.getOfflinePlayer(targetUuid).getName() + "'s Cosmetics" :
                "§8Your Cosmetic Backpack";

        inventory = Bukkit.createInventory(null, SLOTS, title);

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
        List<String> ownedCosmeticIds = plugin.getDatabaseManager().getCosmeticRepository().getPlayerCosmeticIds(targetUuid);
        List<String> equippedCosmeticIds = plugin.getDatabaseManager().getCosmeticRepository().getPlayerEquippedCosmeticIds(targetUuid);

        // Get all cosmetics from config
        List<Cosmetic> allCosmetics = plugin.getConfigManager().getAllCosmetics();

        // Filter cosmetics if needed
        List<Cosmetic> filteredCosmetics = new ArrayList<>();
        for (Cosmetic cosmetic : allCosmetics) {
            if (ownedCosmeticIds.contains(cosmetic.id())) {
                if (currentFilter == null || cosmetic.type() == currentFilter) {
                    filteredCosmetics.add(cosmetic);
                }
            }
        }

        // Calculate pagination
        int totalPages = (int) Math.ceil((double) filteredCosmetics.size() / COSMETIC_SLOTS);
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }

        // Add cosmetics to inventory
        int startIndex = currentPage * COSMETIC_SLOTS;
        int endIndex = Math.min(startIndex + COSMETIC_SLOTS, filteredCosmetics.size());

        for (int i = startIndex; i < endIndex; i++) {
            Cosmetic cosmetic = filteredCosmetics.get(i);
            int slot = i - startIndex;

            // Create item representation
            ItemStack item = createCosmeticItem(cosmetic, equippedCosmeticIds.contains(cosmetic.id()));

            // Add to inventory
            inventory.setItem(slot, item);
            slotToCosmeticId.put(slot, cosmetic.id());
        }

        // Add navigation items
        addNavigationItems(totalPages);
    }

    private ItemStack createCosmeticItem(Cosmetic cosmetic, boolean equipped) {
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
            meta.setDisplayName((equipped ? "§a§l[EQUIPPED] " : "") + "§r" + cosmetic.name());

            // Add rarity and description to lore
            List<String> lore = new ArrayList<>(cosmetic.description());
            lore.add("");
            lore.add("§7Rarity: " + cosmetic.getRarityColor() + cosmetic.rarity());
            if (!cosmetic.tradeable()) {
                lore.add("§c§lNOT TRADEABLE");
            }

            // Add equip/unequip instructions if not view-only
            if (!viewOnly) {
                lore.add("");
                if (equipped) {
                    lore.add("§c§lClick to unequip");
                } else {
                    lore.add("§a§lClick to equip");
                }
            }

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
        ItemStack toolsFilter = createFilterButton(CosmeticType.PICKAXE, "Tools");
        ItemStack weaponsFilter = createFilterButton(CosmeticType.SWORD, "Weapons");
        ItemStack chatColorFilter = createFilterButton(CosmeticType.CHAT_COLOR, "Chat Colors");
        ItemStack namePaintFilter = createFilterButton(CosmeticType.NAME_PAINT, "Name Paints");

        inventory.setItem(SLOTS - 9, allFilter);
        inventory.setItem(SLOTS - 8, toolsFilter);
        inventory.setItem(SLOTS - 7, weaponsFilter);
        inventory.setItem(SLOTS - 6, chatColorFilter);
        inventory.setItem(SLOTS - 5, namePaintFilter);

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

    private ItemStack createFilterButton(CosmeticType type, String name) {
        Material material;
        boolean selected = (currentFilter == type);

        if (type == null) {
            material = Material.CHEST;
        } else {
            switch (type) {
                case PICKAXE:
                    material = Material.DIAMOND_PICKAXE;
                    break;
                case SWORD:
                    material = Material.DIAMOND_SWORD;
                    break;
                case CHAT_COLOR:
                    material = Material.NAME_TAG;
                    break;
                case NAME_PAINT:
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
        if (slot >= SLOTS - 9) {
            handleNavigationClick(clicker, slot);
            return;
        }

        // Handle cosmetic click
        if (slotToCosmeticId.containsKey(slot)) {
            String cosmeticId = slotToCosmeticId.get(slot);
            handleCosmeticClick(clicker, cosmeticId);
        }
    }

    private void handleNavigationClick(Player clicker, int slot) {
        // Filter buttons
        if (slot == SLOTS - 9) {
            // All cosmetics
            currentFilter = null;
            loadCosmetics();
        } else if (slot == SLOTS - 8) {
            // Tools filter
            currentFilter = CosmeticType.PICKAXE;
            loadCosmetics();
        } else if (slot == SLOTS - 7) {
            // Weapons filter
            currentFilter = CosmeticType.SWORD;
            loadCosmetics();
        } else if (slot == SLOTS - 6) {
            // Chat colors filter
            currentFilter = CosmeticType.CHAT_COLOR;
            loadCosmetics();
        } else if (slot == SLOTS - 5) {
            // Name paints filter
            currentFilter = CosmeticType.NAME_PAINT;
            loadCosmetics();
        }

        // Pagination
        else if (slot == SLOTS - 3) {
            // Previous page
            if (currentPage > 0) {
                currentPage--;
                loadCosmetics();
            }
        } else if (slot == SLOTS - 1) {
            // Next page
            currentPage++;
            loadCosmetics();
        }
    }

    private void handleCosmeticClick(Player clicker, String cosmeticId) {
        if (viewOnly || !clicker.getUniqueId().equals(targetUuid)) {
            return;
        }

        // Check if cosmetic is equipped
        boolean isEquipped = plugin.getDatabaseManager().getCosmeticRepository()
                .isEquipped(clicker.getUniqueId(), cosmeticId);

        if (isEquipped) {
            // Unequip cosmetic
            plugin.getDatabaseManager().getCosmeticRepository()
                    .unequipCosmetic(clicker.getUniqueId(), cosmeticId);
            clicker.sendMessage("§cCosmetic unequipped!");

            // Notify other servers
            plugin.getSyncManager().notifyCosmeticUnequipped(clicker.getUniqueId(), cosmeticId);
        } else {
            // Equip cosmetic
            plugin.getDatabaseManager().getCosmeticRepository()
                    .equipCosmetic(clicker.getUniqueId(), cosmeticId);
            clicker.sendMessage("§aCosmetic equipped!");

            // Notify other servers
            plugin.getSyncManager().notifyCosmeticEquipped(clicker.getUniqueId(), cosmeticId);
        }

        // Reload the inventory to reflect changes
        loadCosmetics();
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
