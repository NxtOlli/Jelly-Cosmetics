package com.jellymc.cosmetics.velocity.sync;

import com.jellymc.cosmetics.core.sync.SyncMessage;
import com.jellymc.cosmetics.velocity.JellyCosmeticsVelocity;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class VelocitySyncManager {

    private final JellyCosmeticsVelocity plugin;
    private JedisPool jedisPool;
    private final String CHANNEL_PREFIX = "jellymc:cosmetics:";
    private boolean useRedis;
    private final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("velocity:jellycosmetics");

    public VelocitySyncManager(JellyCosmeticsVelocity plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        useRedis = plugin.getConfigManager().getConfig().getNode("sync", "redis", "enabled").getBoolean(false);

        if (useRedis) {
            String host = plugin.getConfigManager().getConfig().getNode("sync", "redis", "host").getString("localhost");
            int port = plugin.getConfigManager().getConfig().getNode("sync", "redis", "port").getInt(6379);
            String password = plugin.getConfigManager().getConfig().getNode("sync", "redis", "password").getString("");

            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(8);
            poolConfig.setMaxIdle(8);
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
            // Register for plugin messages
            plugin.getServer().getEventManager().register(plugin, this);
            plugin.getLogger().info("Plugin messaging sync system initialized!");
        }
    }

    private void startRedisSubscriber() {
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
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

                // Try to reconnect after a delay
                plugin.getServer().getScheduler().buildTask(plugin, this::startRedisSubscriber)
                        .delay(5, TimeUnit.SECONDS)
                        .schedule();
            }
        }).schedule();
    }

    private void handleRedisMessage(String message) {
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            SyncMessage syncMessage = SyncMessage.deserialize(message);

            // Forward the message to all servers
            forwardMessageToServers(syncMessage);
        }).schedule();
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) {
            return;
        }

        // Prevent loops
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }

        // Read the message
        byte[] data = event.getData();
        String messageJson = new String(data, StandardCharsets.UTF_8);

        try {
            SyncMessage message = SyncMessage.deserialize(messageJson);

            // Forward to Redis if enabled, otherwise to all servers
            if (useRedis) {
                sendRedisMessage(message);
            } else {
                forwardMessageToServers(message, (ServerConnection) event.getSource());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error processing plugin message: " + e.getMessage());
        }
    }

    private void forwardMessageToServers(SyncMessage message) {
        forwardMessageToServers(message, null);
    }

    private void forwardMessageToServers(SyncMessage message, ServerConnection sourceServer) {
        byte[] data = message.serialize().getBytes(StandardCharsets.UTF_8);

        // Send to all servers except the source
        plugin.getServer().getAllServers().forEach(server -> {
            if (sourceServer != null && server.equals(sourceServer.getServer())) {
                return;
            }

            server.sendPluginMessage(CHANNEL, data);
        });
    }

    public void sendSyncMessage(SyncMessage message) {
        if (useRedis) {
            sendRedisMessage(message);
        } else {
            // Forward to all servers
            forwardMessageToServers(message);
        }
    }

    private void sendRedisMessage(SyncMessage message) {
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(CHANNEL_PREFIX + message.getChannel().name().toLowerCase(), message.serialize());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to publish Redis message: " + e.getMessage());
            }
        }).schedule();
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
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
}