package com.jellymc.cosmetics.core.sync;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.UUID;

/**
 * Represents a message that can be synchronized across servers
 */
public class SyncMessage {
    private static final Gson GSON = new Gson();

    private final SyncChannel channel;
    private final UUID playerUuid;
    private final String action;
    private final String data;

    /**
     * Creates a new sync message
     *
     * @param channel The channel this message belongs to
     * @param playerUuid The UUID of the player this message is about
     * @param action The action being performed
     * @param data Additional data for the action
     */
    public SyncMessage(SyncChannel channel, UUID playerUuid, String action, String data) {
        this.channel = channel;
        this.playerUuid = playerUuid;
        this.action = action;
        this.data = data;
    }

    /**
     * Gets the channel this message belongs to
     *
     * @return The sync channel
     */
    public SyncChannel getChannel() {
        return channel;
    }

    /**
     * Gets the UUID of the player this message is about
     *
     * @return The player UUID
     */
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    /**
     * Gets the action being performed
     *
     * @return The action
     */
    public String getAction() {
        return action;
    }

    /**
     * Gets the additional data for the action
     *
     * @return The data
     */
    public String getData() {
        return data;
    }

    /**
     * Serializes this message to a JSON string
     *
     * @return The serialized message
     */
    public String serialize() {
        JsonObject json = new JsonObject();
        json.addProperty("channel", channel.name());
        json.addProperty("playerUuid", playerUuid.toString());
        json.addProperty("action", action);
        json.addProperty("data", data);
        return GSON.toJson(json);
    }

    /**
     * Deserializes a JSON string to a SyncMessage
     *
     * @param json The JSON string
     * @return The deserialized message
     */
    public static SyncMessage deserialize(String json) {
        JsonObject jsonObject = GSON.fromJson(json, JsonObject.class);
        SyncChannel channel = SyncChannel.valueOf(jsonObject.get("channel").getAsString());
        UUID playerUuid = UUID.fromString(jsonObject.get("playerUuid").getAsString());
        String action = jsonObject.get("action").getAsString();
        String data = jsonObject.get("data").getAsString();
        return new SyncMessage(channel, playerUuid, action, data);
    }

    // Factory methods for common message types

    /**
     * Creates a message for when a cosmetic is given to a player
     *
     * @param playerUuid The player UUID
     * @param cosmeticId The cosmetic ID
     * @return The sync message
     */
    public static SyncMessage cosmeticGiven(UUID playerUuid, String cosmeticId) {
        return new SyncMessage(SyncChannel.COSMETIC, playerUuid, "given", cosmeticId);
    }

    /**
     * Creates a message for when a cosmetic is equipped by a player
     *
     * @param playerUuid The player UUID
     * @param cosmeticId The cosmetic ID
     * @return The sync message
     */
    public static SyncMessage cosmeticEquipped(UUID playerUuid, String cosmeticId) {
        return new SyncMessage(SyncChannel.COSMETIC, playerUuid, "equipped", cosmeticId);
    }

    /**
     * Creates a message for when a cosmetic is unequipped by a player
     *
     * @param playerUuid The player UUID
     * @param cosmeticId The cosmetic ID
     * @return The sync message
     */
    public static SyncMessage cosmeticUnequipped(UUID playerUuid, String cosmeticId) {
        return new SyncMessage(SyncChannel.COSMETIC, playerUuid, "unequipped", cosmeticId);
    }

    /**
     * Creates a message for when a player's chat color changes
     *
     * @param playerUuid The player UUID
     * @param chatColorId The chat color ID
     * @return The sync message
     */
    public static SyncMessage chatColorChanged(UUID playerUuid, String chatColorId) {
        return new SyncMessage(SyncChannel.CHAT_COLOR, playerUuid, "changed", chatColorId);
    }

    /**
     * Creates a message for when a player's name paint changes
     *
     * @param playerUuid The player UUID
     * @param namePaintId The name paint ID
     * @return The sync message
     */
    public static SyncMessage namePaintChanged(UUID playerUuid, String namePaintId) {
        return new SyncMessage(SyncChannel.NAME_PAINT, playerUuid, "changed", namePaintId);
    }

    /**
     * Creates a message for when an auction listing is created
     *
     * @param playerUuid The seller UUID
     * @param listingId The listing ID
     * @return The sync message
     */
    public static SyncMessage auctionListingCreated(UUID playerUuid, int listingId) {
        return new SyncMessage(SyncChannel.AUCTION, playerUuid, "created", String.valueOf(listingId));
    }

    /**
     * Creates a message for when an auction listing is purchased
     *
     * @param playerUuid The buyer UUID
     * @param listingId The listing ID
     * @return The sync message
     */
    public static SyncMessage auctionListingPurchased(UUID playerUuid, int listingId) {
        return new SyncMessage(SyncChannel.AUCTION, playerUuid, "purchased", String.valueOf(listingId));
    }

    /**
     * Creates a message for when an auction listing is cancelled
     *
     * @param playerUuid The seller UUID
     * @param listingId The listing ID
     * @return The sync message
     */
    public static SyncMessage auctionListingCancelled(UUID playerUuid, int listingId) {
        return new SyncMessage(SyncChannel.AUCTION, playerUuid, "cancelled", String.valueOf(listingId));
    }

    /**
     * Creates a message for when an auction listing expires
     *
     * @param playerUuid The seller UUID
     * @param listingId The listing ID
     * @return The sync message
     */
    public static SyncMessage auctionListingExpired(UUID playerUuid, int listingId) {
        return new SyncMessage(SyncChannel.AUCTION, playerUuid, "expired", String.valueOf(listingId));
    }

    /**
     * Creates a message for when a player's resource pack status changes
     *
     * @param playerUuid The player UUID
     * @param status The resource pack status (accepted, declined, loaded, failed)
     * @return The sync message
     */
    public static SyncMessage resourcePackStatus(UUID playerUuid, String status) {
        return new SyncMessage(SyncChannel.RESOURCE_PACK, playerUuid, "status", status);
    }

    /**
     * Creates a message for when a player's cosmetic is removed
     *
     * @param playerUuid The player UUID
     * @param cosmeticId The cosmetic ID
     * @return The sync message
     */
    public static SyncMessage cosmeticRemoved(UUID playerUuid, String cosmeticId) {
        return new SyncMessage(SyncChannel.COSMETIC, playerUuid, "removed", cosmeticId);
    }

    @Override
    public String toString() {
        return "SyncMessage{" +
                "channel=" + channel +
                ", playerUuid=" + playerUuid +
                ", action='" + action + '\'' +
                ", data='" + data + '\'' +
                '}';
    }
}
