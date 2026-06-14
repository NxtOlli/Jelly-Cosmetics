package com.jellymc.cosmetics.spigot.config;

import com.jellymc.cosmetics.core.model.Cosmetic;
import com.jellymc.cosmetics.core.model.CosmeticType;
import com.jellymc.cosmetics.core.model.NamePaint;
import com.jellymc.cosmetics.spigot.JellyCosmeticsPlugin;
import com.jellymc.cosmetics.core.model.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpigotConfigManager {
    private final JellyCosmeticsPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration cosmeticsConfig;
    private FileConfiguration chatColorsConfig;
    private FileConfiguration namePaintsConfig;

    private final Map<String, Cosmetic> cosmeticsById = new HashMap<>();
    private final Map<String, ChatColor> chatColorsById = new HashMap<>();
    private final Map<String, NamePaint> namePaintsById = new HashMap<>();

    public SpigotConfigManager(JellyCosmeticsPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        // Load main config
        plugin.saveDefaultConfig();
        config = plugin.getConfig();

        // Load cosmetics config
        cosmeticsConfig = loadConfig("cosmetics.yml");

        // Load chat colors config
        chatColorsConfig = loadConfig("chat_colors.yml");

        // Load name paints config
        namePaintsConfig = loadConfig("name_paints.yml");

        // Parse configs
        loadCosmetics();
        loadChatColors();
        loadNamePaints();
    }

    private FileConfiguration loadConfig(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource(fileName, false);
        }

        return YamlConfiguration.loadConfiguration(configFile);
    }

    private void loadCosmetics() {
        cosmeticsById.clear();

        ConfigurationSection cosmeticsSection = cosmeticsConfig.getConfigurationSection("cosmetics");
        if (cosmeticsSection == null) return;

        for (String id : cosmeticsSection.getKeys(false)) {
            ConfigurationSection section = cosmeticsSection.getConfigurationSection(id);
            if (section == null) continue;

            String name = section.getString("name", "Unknown Cosmetic");
            String typeStr = section.getString("type", "UNKNOWN");
            CosmeticType type;
            try {
                type = CosmeticType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid cosmetic type: " + typeStr + " for cosmetic: " + id);
                continue;
            }

            String textureId = section.getString("texture_id", "");
            String permission = section.getString("permission", "");
            String rarity = section.getString("rarity", "Common");
            boolean tradeable = section.getBoolean("tradeable", true);
            List<String> description = section.getStringList("description");

            Cosmetic cosmetic = new Cosmetic(id, name, type, textureId, permission, rarity, tradeable, description);
            cosmeticsById.put(id, cosmetic);
        }

        plugin.getLogger().info("Loaded " + cosmeticsById.size() + " cosmetics");
    }

    private void loadChatColors() {
        chatColorsById.clear();

        ConfigurationSection chatColorsSection = chatColorsConfig.getConfigurationSection("chat_colors");
        if (chatColorsSection == null) return;

        for (String id : chatColorsSection.getKeys(false)) {
            ConfigurationSection section = chatColorsSection.getConfigurationSection(id);
            if (section == null) continue;

            String name = section.getString("name", "Unknown Chat Color");
            String colorCode = section.getString("color_code", "§f");
            String permission = section.getString("permission", "");
            boolean animated = section.getBoolean("animated", false);

            if (animated) {
                List<String> frames = section.getStringList("animation_frames");
                int speed = section.getInt("animation_speed", 10);

                ChatColor chatColor = new ChatColor(id, name, colorCode, permission,
                        true, frames.toArray(new String[0]), speed);
                chatColorsById.put(id, chatColor);
            } else {
                ChatColor chatColor = new ChatColor(id, name, colorCode, permission);
                chatColorsById.put(id, chatColor);
            }
        }

        plugin.getLogger().info("Loaded " + chatColorsById.size() + " chat colors");
    }

    private void loadNamePaints() {
        namePaintsById.clear();

        ConfigurationSection namePaintsSection = namePaintsConfig.getConfigurationSection("name_paints");
        if (namePaintsSection == null) return;

        for (String id : namePaintsSection.getKeys(false)) {
            ConfigurationSection section = namePaintsSection.getConfigurationSection(id);
            if (section == null) continue;

            String name = section.getString("name", "Unknown Name Paint");
            String format = section.getString("format", "§f{name}");
            String permission = section.getString("permission", "");
            boolean animated = section.getBoolean("animated", false);

            if (animated) {
                List<String> frames = section.getStringList("animation_frames");
                int speed = section.getInt("animation_speed", 10);

                NamePaint namePaint = new NamePaint(id, name, format, permission,
                        true, frames.toArray(new String[0]), speed);
                namePaintsById.put(id, namePaint);
            } else {
                NamePaint namePaint = new NamePaint(id, name, format, permission);
                namePaintsById.put(id, namePaint);
            }
        }

        plugin.getLogger().info("Loaded " + namePaintsById.size() + " name paints");
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getCosmeticsConfig() {
        return cosmeticsConfig;
    }

    public FileConfiguration getChatColorsConfig() {
        return chatColorsConfig;
    }

    public FileConfiguration getNamePaintsConfig() {
        return namePaintsConfig;
    }

    public List<Cosmetic> getAllCosmetics() {
        return new ArrayList<>(cosmeticsById.values());
    }

    public Cosmetic getCosmeticById(String id) {
        return cosmeticsById.get(id);
    }

    public List<com.jellymc.cosmetics.core.model.ChatColor> getAllChatColors() {
        return new ArrayList<>(chatColorsById.values());
    }

    public com.jellymc.cosmetics.core.model.ChatColor getChatColorById(String id) {
        return chatColorsById.get(id);
    }

    public List<NamePaint> getAllNamePaints() {
        return new ArrayList<>(namePaintsById.values());
    }

    public NamePaint getNamePaintById(String id) {
        return namePaintsById.get(id);
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        cosmeticsConfig = loadConfig("cosmetics.yml");
        chatColorsConfig = loadConfig("chat_colors.yml");
        namePaintsConfig = loadConfig("name_paints.yml");

        loadCosmetics();
        loadChatColors();
        loadNamePaints();
    }
}
