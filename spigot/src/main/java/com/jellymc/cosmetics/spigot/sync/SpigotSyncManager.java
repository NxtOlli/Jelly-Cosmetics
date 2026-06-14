package com.jellymc.cosmetics.spigot.sync;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.jellymc.cosmetics.core.sync.SyncMessage;
import com.jellymc.cosmetics.spigot.JellyCosmeticsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;

public class SpigotSyncManager {
    private final JellyCosmeticsPlugin plugin;
    private JedisPool jedisPool;
    private final String CHANNEL_PREFIX = "jellymc:cosmetics:";
    private boolean useRedis;

    public SpigotSyncManager(JellyCosmeticsPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        useRedis = plugin.getConfig().getBoolean("sync.redis.enabled", false);

        if (useRedis) {
            String host = plugin.getConfig().getString("sync.redis.host", "localhost");
            int port = plugin.getConfig().getInt("sync.redis.port", 6379);
            String password = plugin.getConfig().getString("sync.redis.password", "");

            JedisPoolConfig poolConfig = new JedisPoolConfig();
              poolConfig.setMaxTotal(8);
              poolConfig.setMaxTotal(8);
              poolConfig.setMinIdle(0);

            if (password.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, host, port, 2000);
            } else {
                jedisPool = new JedisPool(poolConfig, host, port, 2000, password);
            }

            // Start listening for messages
            startRedisSubscriber();

            plugin.getLogger().info("Redis sync system initialized!");
        } else {
            // Register plugin messaging channels
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "velocity:jellycosmetics");
            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "velocity:jellycosmetics",
                    new PluginMessageManager(plugin));

            plugin.getLogger().info("Plugin messaging sync system initialized!");
        }
    }

    private void startRedisSubscriber() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        if (channel.startsWith(CHANNEL_PREFIX)) {
                            handleRedisMessage(message);
                        }
                    }
                }, CHANNEL_PREFIX + "*");
            } catch (Exception e) {
                plugin.getLogger().severe("Redis subscription error: " + e.getMessage());
                e.printStackTrace();

                // Try to reconnect after a delay
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::startRedisSubscriber, 100L);
            }
        });
    }

    void handleRedisMessage(String message) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            SyncMessage syncMessage = SyncMessage.deserialize(message);

            UUID playerUuid = syncMessage.getPlayerUuid();
            String action = syncMessage.getAction();
            String data = syncMessage.getData();

            switch (syncMessage.getChannel()) {
                case COSMETIC:
                    if (action.equals("equipped")) {
                        // Handle cosmetic equipped event
                        Player player = Bukkit.getPlayer(playerUuid);
                        if (player != null) {
                            // Update player's visual appearance
                        }
                    } else if (action.equals("unequipped")) {
                        // Handle cosmetic unequipped event
                        Player player = Bukkit.getPlayer(playerUuid);
                        if (player != null) {
                            // Update player's visual appearance
                        }
                    }
                    break;
                case CHAT_COLOR:
                    if (action.equals("changed")) {
                        // Handle chat color change event
                    }
                    break;
                case NAME_PAINT:
                    if (action.equals("changed")) {
                        // Handle name paint change event
                        Player player = Bukkit.getPlayer(playerUuid);
                        if (player != null) {
                            // Update player's display name
                            String namePaintId = data;
                            if (namePaintId != null && !namePaintId.isEmpty()) {
                                // Apply name paint
                                plugin.getDatabaseManager().getCosmeticRepository()
                                        .setPlayerNamePaint(playerUuid, namePaintId);
                            } else {
                                // Reset name paint
                                plugin.getDatabaseManager().getCosmeticRepository()
                                        .setPlayerNamePaint(playerUuid, null);
                                player.setDisplayName(player.getName());
                            }
                        }
                    }
                    break;
            }
        });
    }

    public void sendSyncMessage(SyncMessage message) {
        if (useRedis) {
            sendRedisMessage(message);
        } else {
            sendPluginMessage(message);
        }
    }

    private void sendRedisMessage(SyncMessage message) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(CHANNEL_PREFIX + message.getChannel().name().toLowerCase(), message.serialize());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to publish Redis message: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void sendPluginMessage(SyncMessage message) {
        // Find an online player to send the message through
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            plugin.getLogger().warning("Cannot send plugin message: no players online");
            return;
        }

        Player player = Bukkit.getOnlinePlayers().iterator().next();

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL"); // Send to all servers
        out.writeUTF("JellyCosmetics");

        // Write the message data
        byte[] messageBytes = message.serialize().getBytes();
        out.writeShort(messageBytes.length);
        out.write(messageBytes);

        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void notifyCosmeticGiven(UUID playerUuid, String cosmeticId) {
        sendSyncMessage(SyncMessage.cosmeticGiven(playerUuid, cosmeticId));
    }

    public void notifyCosmeticEquipped(UUID playerUuid, String cosmeticId) {
        sendSyncMessage(SyncMessage.cosmeticEquipped(playerUuid, cosmeticId));
    }

    public void notifyCosmeticUnequipped(UUID playerUuid, String cosmeticId) {
        sendSyncMessage(SyncMessage.cosmeticUnequipped(playerUuid, cosmeticId));
    }

    public void notifyChatColorChanged(UUID playerUuid, String chatColorId) {
        sendSyncMessage(SyncMessage.chatColorChanged(playerUuid, chatColorId));
    }

    public void notifyNamePaintChanged(UUID playerUuid, String namePaintId) {
        sendSyncMessage(SyncMessage.namePaintChanged(playerUuid, namePaintId));
    }

    public void shutdown() {
        if (jedisPool != null && jedisPool.getResource().isConnected()) {
            jedisPool.close();
        }
    }
}
