package com.jellymc.cosmetics.velocity.command;

import com.jellymc.cosmetics.velocity.JellyCosmeticsVelocity;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class VelocityCosmeticAdminCommand implements SimpleCommand {
    private final JellyCosmeticsVelocity plugin;

    public VelocityCosmeticAdminCommand(JellyCosmeticsVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("jellycosmetics.admin")) {
            source.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return;
        }

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("This command can only be used by players.").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) source;

        if (args.length == 0) {
            sendAdminHelpMessage(source);
            return;
        }

        // Forward all admin commands to the current server
        if (player.getCurrentServer().isPresent()) {
            StringBuilder command = new StringBuilder("cosmeticadmin");
            for (String arg : args) {
                command.append(" ").append(arg);
            }
            player.spoofChatInput(command.toString());
        } else {
            player.sendMessage(Component.text("You must be connected to a server to use this command.").color(NamedTextColor.RED));
        }
    }

    private void sendAdminHelpMessage(CommandSource source) {
        source.sendMessage(Component.text("=== JellyMC Cosmetics Admin Help ===").color(NamedTextColor.GOLD));
        source.sendMessage(Component.text("/cosmeticadmin give <player> <cosmetic_id> - Give a cosmetic to a player").color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("/cosmeticadmin remove <player> <cosmetic_id> - Remove a cosmetic from a player").color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("/cosmeticadmin list <player> - List a player's cosmetics").color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("/cosmeticadmin reload - Reload configuration").color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("/cosmeticadmin logs - View recent logs").color(NamedTextColor.YELLOW));
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        List<String> suggestions = new ArrayList<>();

        if (!invocation.source().hasPermission("jellycosmetics.admin")) {
            return CompletableFuture.completedFuture(suggestions);
        }

        if (args.length == 0 || args.length == 1) {
            suggestions.add("give");
            suggestions.add("remove");
            suggestions.add("list");
            suggestions.add("reload");
            suggestions.add("logs");
            suggestions.add("help");
            return CompletableFuture.completedFuture(filterStartingWith(suggestions, args.length > 0 ? args[0] : ""));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") ||
                    args[0].equalsIgnoreCase("remove") ||
                    args[0].equalsIgnoreCase("list")) {
                // Suggest online players
                List<String> playerNames = new ArrayList<>();
                plugin.getServer().getAllPlayers().forEach(p -> playerNames.add(p.getUsername()));
                return CompletableFuture.completedFuture(filterStartingWith(playerNames, args[1]));
            }
        }

        return CompletableFuture.completedFuture(suggestions);
    }

    private List<String> filterStartingWith(List<String> list, String prefix) {
        if (prefix.isEmpty()) {
            return list;
        }

        List<String> filtered = new ArrayList<>();
        for (String item : list) {
            if (item.toLowerCase().startsWith(prefix.toLowerCase())) {
                filtered.add(item);
            }
        }
        return filtered;
    }
}
