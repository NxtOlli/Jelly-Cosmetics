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

public class VelocityCosmeticCommand implements SimpleCommand {

    private final JellyCosmeticsVelocity plugin;

    public VelocityCosmeticCommand(JellyCosmeticsVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("This command can only be used by players.").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) source;

        if (args.length == 0) {
            // Forward to the current server
            forwardCommandToServer(player, "cosmetic");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "view":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /cosmetic view <player>").color(NamedTextColor.RED));
                    return;
                }

                // Forward to the current server
                forwardCommandToServer(player, "cosmetic view " + args[1]);
                break;

            case "chatcolor":
                // Forward to the current server
                forwardCommandToServer(player, "cosmetic chatcolor");
                break;

            case "namepaint":
                // Forward to the current server
                forwardCommandToServer(player, "cosmetic namepaint");
                break;

            case "ah":
            case "auction":
                // Forward to the current server
                forwardCommandToServer(player, "cosmetic ah");
                break;

            case "help":
                sendHelpMessage(player);
                break;

            default:
                player.sendMessage(Component.text("Unknown subcommand. Use /cosmetic help for help.").color(NamedTextColor.RED));
                break;
        }
    }

    private void forwardCommandToServer(Player player, String command) {
        if (player.getCurrentServer().isPresent()) {
            player.spoofChatInput(command);
        } else {
            player.sendMessage(Component.text("You must be connected to a server to use this command.").color(NamedTextColor.RED));
        }
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(Component.text("=== JellyMC Cosmetics Help ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/cosmetic - Open the cosmetic menu").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/cosmetic view <player> - View another player's cosmetics").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/cosmetic chatcolor - Change your chat color").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/cosmetic namepaint - Change your name paint").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/cosmetic ah - Open the auction house").color(NamedTextColor.YELLOW));
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        List<String> suggestions = new ArrayList<>();

        if (args.length == 0 || args.length == 1) {
            suggestions.add("view");
            suggestions.add("chatcolor");
            suggestions.add("namepaint");
            suggestions.add("ah");
            suggestions.add("help");
            return CompletableFuture.completedFuture(filterStartingWith(suggestions, args.length > 0 ? args[0] : ""));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("view")) {
            // Suggest online players
            List<String> playerNames = new ArrayList<>();
            plugin.getServer().getAllPlayers().forEach(p -> playerNames.add(p.getUsername()));
            return CompletableFuture.completedFuture(filterStartingWith(playerNames, args[1]));
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
