package com.jellymc.cosmetics.core.config;

public class ConfigKeys {
    // Database configuration
    public static final String DATABASE_HOST = "database.host";
    public static final String DATABASE_PORT = "database.port";
    public static final String DATABASE_NAME = "database.name";
    public static final String DATABASE_USERNAME = "database.username";
    public static final String DATABASE_PASSWORD = "database.password";

    // Redis configuration
    public static final String REDIS_ENABLED = "sync.redis.enabled";
    public static final String REDIS_HOST = "sync.redis.host";
    public static final String REDIS_PORT = "sync.redis.port";
    public static final String REDIS_PASSWORD = "sync.redis.password";

    // Resource pack configuration
    public static final String RESOURCE_PACK_URL = "resourcepack.url";
    public static final String RESOURCE_PACK_HASH = "resourcepack.hash";
    public static final String RESOURCE_PACK_FORCE = "resourcepack.force";
    public static final String RESOURCE_PACK_PROMPT = "resourcepack.prompt";
    public static final String RESOURCE_PACK_HUB_SERVERS = "resourcepack.hub_servers";

    // Auction house configuration
    public static final String AUCTION_LISTING_FEE = "auction.listing_fee";
    public static final String AUCTION_LISTING_DURATION = "auction.listing_duration";
    public static final String AUCTION_MIN_PRICE = "auction.min_price";
    public static final String AUCTION_MAX_PRICE = "auction.max_price";

    // Chat color configuration
    public static final String CHAT_COLORS_ENABLED = "chat_colors.enabled";
    public static final String CHAT_COLORS_DEFAULT_PERMISSION = "chat_colors.default_permission";

    // Name paint configuration
    public static final String NAME_PAINTS_ENABLED = "name_paints.enabled";
    public static final String NAME_PAINTS_DEFAULT_PERMISSION = "name_paints.default_permission";
    public static final String NAME_PAINTS_UPDATE_INTERVAL = "name_paints.update_interval";

    // Permission nodes
    public static final String PERMISSION_USE = "jellycosmetics.use";
    public static final String PERMISSION_VIEW = "jellycosmetics.view";
    public static final String PERMISSION_CHATCOLOR = "jellycosmetics.chatcolor";
    public static final String PERMISSION_NAMEPAINT = "jellycosmetics.namepaint";
    public static final String PERMISSION_AUCTION = "jellycosmetics.auction";
    public static final String PERMISSION_ADMIN = "jellycosmetics.admin";

    private ConfigKeys() {
        // Private constructor to prevent instantiation
    }
}
