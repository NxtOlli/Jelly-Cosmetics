package com.jellymc.cosmetics.spigot.sync;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.jellymc.cosmetics.core.sync.SyncMessage;
import com.jellymc.cosmetics.spigot.JellyCosmeticsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;

public class PluginMessageManager implements PluginMessageListener {
    private final JellyCosmeticsPlugin plugin;

    public PluginMessageManager(JellyCosmeticsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("velocity:jellycosmetics")) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("JellyCosmetics")) {
            short len = in.readShort();
            byte[] msgbytes = new byte[len];
            in.readFully(msgbytes);

            String jsonMessage = new String(msgbytes, StandardCharsets.UTF_8);

            try {
                SyncMessage syncMessage = SyncMessage.deserialize(jsonMessage);
                handleSyncMessage(syncMessage);
            } catch (Exception e) {
                plugin.getLogger().severe("Error processing plugin message: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void handleSyncMessage(SyncMessage message) {
        // Process the message - this is similar to the Redis handler
        plugin.getSyncManager().handleRedisMessage(message.serialize());
    }
}
