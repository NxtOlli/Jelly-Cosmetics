package com.jellymc.cosmetics.core.model;

import java.util.List;

public record Cosmetic(String id, String name, CosmeticType type, String textureId, String permission, String rarity,
                       boolean tradeable, List<String> description) {

    public String getRarityColor() {
        return switch (rarity.toLowerCase()) {
            case "common" -> "§7";
            case "uncommon" -> "§a";
            case "rare" -> "§9";
            case "epic" -> "§5";
            case "legendary" -> "§6";
            case "mythic" -> "§d";
            default -> "§f";
        };
    }
}
