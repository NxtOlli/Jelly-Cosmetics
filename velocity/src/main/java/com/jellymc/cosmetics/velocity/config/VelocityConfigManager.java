package com.jellymc.cosmetics.velocity.config;

import com.jellymc.cosmetics.velocity.JellyCosmeticsVelocity;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class VelocityConfigManager {

    private final JellyCosmeticsVelocity plugin;
    private final Path dataDirectory;
    private ConfigurationNode config;
    private ConfigurationNode cosmeticsConfig;
    private ConfigurationNode chatColorsConfig;
    private ConfigurationNode namePaintsConfig;

    public VelocityConfigManager(JellyCosmeticsVelocity plugin, Path dataDirectory) {
        this.plugin = plugin;
        this.dataDirectory = dataDirectory;
    }

    public void loadConfigs() {
        try {
            // Create data directory if it doesn't exist
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }

            // Load main config
            Path configPath = dataDirectory.resolve("config.yml");
            if (!Files.exists(configPath)) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (in != null) {
                        Files.copy(in, configPath);
                    } else {
                        Files.createFile(configPath);
                    }
                }
            }

            config = YAMLConfigurationLoader.builder().setPath(configPath).build().load();

            // Load cosmetics config
            Path cosmeticsPath = dataDirectory.resolve("cosmetics.yml");
            if (!Files.exists(cosmeticsPath)) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("cosmetics.yml")) {
                    if (in != null) {
                        Files.copy(in, cosmeticsPath);
                    } else {
                        Files.createFile(cosmeticsPath);
                    }
                }
            }

            cosmeticsConfig = YAMLConfigurationLoader.builder().setPath(cosmeticsPath).build().load();

            // Load chat colors config
            Path chatColorsPath = dataDirectory.resolve("chat_colors.yml");
            if (!Files.exists(chatColorsPath)) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("chat_colors.yml")) {
                    if (in != null) {
                        Files.copy(in, chatColorsPath);
                    } else {
                        Files.createFile(chatColorsPath);
                    }
                }
            }

            chatColorsConfig = YAMLConfigurationLoader.builder().setPath(chatColorsPath).build().load();

            // Load name paints config
            Path namePaintsPath = dataDirectory.resolve("name_paints.yml");
            if (!Files.exists(namePaintsPath)) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("name_paints.yml")) {
                    if (in != null) {
                        Files.copy(in, namePaintsPath);
                    } else {
                        Files.createFile(namePaintsPath);
                    }
                }
            }

            namePaintsConfig = YAMLConfigurationLoader.builder().setPath(namePaintsPath).build().load();

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load configuration files: " + e.getMessage());
        }
    }

    public ConfigurationNode getConfig() {
        return config;
    }

    public ConfigurationNode getCosmeticsConfig() {
        return cosmeticsConfig;
    }

    public ConfigurationNode getChatColorsConfig() {
        return chatColorsConfig;
    }

    public ConfigurationNode getNamePaintsConfig() {
        return namePaintsConfig;
    }

    public List<String> getHubServers() {
        List<String> hubServers = new ArrayList<>();
        try {
            hubServers = config.getNode("resourcepack", "hub_servers").getList(Object::toString);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load hub servers list, using default");
            hubServers.add("hub");
        }
        return hubServers;
    }

    public void reloadConfigs() {
        loadConfigs();
    }
}