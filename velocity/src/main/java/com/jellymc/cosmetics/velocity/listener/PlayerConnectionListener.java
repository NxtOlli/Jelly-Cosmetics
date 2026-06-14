package com.jellymc.cosmetics.velocity.listener;

import com.jellymc.cosmetics.velocity.JellyCosmeticsVelocity;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;

public class PlayerConnectionListener {

    private final JellyCosmeticsVelocity plugin;

    public PlayerConnectionListener(JellyCosmeticsVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        // Player joined the network
        plugin.getLogger().info("Player " + event.getPlayer().getUsername() + " connected to the network");
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        // Player connected to a server
        plugin.getResourcePackManager().onPlayerJoinServer(
                event.getPlayer(),
                event.getServer()
        );
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        // Player left the network
        plugin.getResourcePackManager().resetPlayerPackStatus(event.getPlayer().getUniqueId());
    }
}