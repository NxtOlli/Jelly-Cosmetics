package com.jellymc.cosmetics.core.util;

import com.jellymc.cosmetics.core.model.ChatColor;
import com.jellymc.cosmetics.core.model.NamePaint;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageFormatter {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("\\{#([A-Fa-f0-9]{6})>(#[A-Fa-f0-9]{6})}(.*?)\\{/gradient}");

    private final Map<UUID, ChatColor> playerChatColors = new HashMap<>();
    private final Map<UUID, NamePaint> playerNamePaints = new HashMap<>();
    private final Map<UUID, Long> lastAnimationTick = new HashMap<>();

    /**
     * Sets a player's chat color
     *
     * @param playerUuid The player UUID
     * @param chatColor The chat color to set
     */
    public void setPlayerChatColor(UUID playerUuid, ChatColor chatColor) {
        if (chatColor == null) {
            playerChatColors.remove(playerUuid);
        } else {
            playerChatColors.put(playerUuid, chatColor);
        }
    }

    /**
     * Sets a player's name paint
     *
     * @param playerUuid The player UUID
     * @param namePaint The name paint to set
     */
    public void setPlayerNamePaint(UUID playerUuid, NamePaint namePaint) {
        if (namePaint == null) {
            playerNamePaints.remove(playerUuid);
        } else {
            playerNamePaints.put(playerUuid, namePaint);
        }
    }

    /**
     * Updates the animation tick for a player
     *
     * @param playerUuid The player UUID
     * @param tick The current tick
     */
    public void updateAnimationTick(UUID playerUuid, long tick) {
        lastAnimationTick.put(playerUuid, tick);
    }

    /**
     * Gets the animation tick for a player
     *
     * @param playerUuid The player UUID
     * @return The animation tick
     */
    public long getAnimationTick(UUID playerUuid) {
        return lastAnimationTick.getOrDefault(playerUuid, 0L);
    }

    /**
     * Formats a chat message with the player's chat color
     *
     * @param playerUuid The player UUID
     * @param message The message to format
     * @return The formatted message
     */
    public String formatChatMessage(UUID playerUuid, String message) {
        ChatColor chatColor = playerChatColors.get(playerUuid);
        if (chatColor == null) {
            return message;
        }

        String colorCode;
        if (chatColor.isAnimated()) {
            long tick = lastAnimationTick.getOrDefault(playerUuid, 0L);
            colorCode = chatColor.getCurrentFrame(tick);
        } else {
            colorCode = chatColor.getColorCode();
        }

        return colorCode + message;
    }

    /**
     * Formats a player name with their name paint
     *
     * @param playerUuid The player UUID
     * @param playerName The player name
     * @return The formatted name
     */
    public String formatPlayerName(UUID playerUuid, String playerName) {
        NamePaint namePaint = playerNamePaints.get(playerUuid);
        if (namePaint == null) {
            return playerName;
        }

        if (namePaint.isAnimated()) {
            long tick = lastAnimationTick.getOrDefault(playerUuid, 0L);
            return namePaint.formatNameWithFrame(playerName, tick);
        } else {
            return namePaint.formatName(playerName);
        }
    }

    /**
     * Converts hex color codes in a message to Minecraft color codes
     *
     * @param message The message to convert
     * @return The converted message
     */
    public static String translateHexColorCodes(String message) {
        if (message == null) return null;

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            // Convert hex to the closest Minecraft color code
            // This is a simplified version - in reality you'd need to map RGB values
            matcher.appendReplacement(buffer, "§" + getClosestMinecraftColor(hex));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Gets the closest Minecraft color code for a hex color
     *
     * @param hex The hex color
     * @return The closest Minecraft color code
     */
    private static String getClosestMinecraftColor(String hex) {
        // This is a simplified implementation
        // In reality, you'd calculate the closest color based on RGB distance

        // For now, just return a default color
        return "f";
    }

    /**
     * Applies a gradient to text
     *
     * @param message The message to format
     * @return The formatted message
     */
    public static String applyGradients(String message) {
        if (message == null) return null;

        Matcher matcher = GRADIENT_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String startColor = matcher.group(1);
            String endColor = matcher.group(2);
            String text = matcher.group(3);

            // Apply gradient to the text
            // This is a simplified version - in reality you'd interpolate colors
            matcher.appendReplacement(buffer, "§f" + text);
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Cleans up resources for a player
     *
     * @param playerUuid The player UUID
     */
    public void cleanup(UUID playerUuid) {
        playerChatColors.remove(playerUuid);
        playerNamePaints.remove(playerUuid);
        lastAnimationTick.remove(playerUuid);
    }
}
