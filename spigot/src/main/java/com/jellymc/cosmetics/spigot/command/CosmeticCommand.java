package com.jellymc.cosmetics.spigot.command;

import com.jellymc.cosmetics.spigot.JellyCosmeticsPlugin;
import com.jellymc.cosmetics.spigot.gui.AuctionHouseGUI;
import com.jellymc.cosmetics.spigot.gui.ChatColorGUI;
import com.jellymc.cosmetics.spigot.gui.CosmeticInventoryGUI;
import com.jellymc.cosmetics.spigot.gui.NamePaintGUI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CosmeticCommand implements CommandExecutor, TabCompleter {

    private final JellyCosmeticsPlugin plugin;

    public CosmeticCommand(JellyCosmeticsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("jellycosmetics.use")) {
            player.sendMessage("§cYou don't have permission to use cosmetics.");
            return true;
        }

        if (args.length == 0) {
            // Open main cosmetic inventory
            new CosmeticInventoryGUI(plugin, player).open();
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "view":
                if (!player.hasPermission("jellycosmetics.view")) {
                    player.sendMessage("§cYou don't have permission to view other players' cosmetics.");
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage("§cUsage: /cosmetic view <player>");
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage("§cPlayer not found.");
                    return true;
                }

                // Open view-only cosmetic inventory for target player
                new CosmeticInventoryGUI(plugin, player, target.getUniqueId(), true).open();
                return true;

            case "chatcolor":
                if (!plugin.getConfig().getBoolean("chat_colors.enabled", true)) {
                    player.sendMessage("§cChat colors are disabled on this server.");
                    return true;
                }

                if (!player.hasPermission("jellycosmetics.chatcolor")) {
                    player.sendMessage("§cYou don't have permission to use chat colors.");
                    return true;
                }

                // Open chat color selection GUI
                new ChatColorGUI(plugin, player).open();
                return true;

            case "namepaint":
                if (!plugin.getConfig().getBoolean("name_paints.enabled", true)) {
                    player.sendMessage("§cName paints are disabled on this server.");
                    return true;
                }

                if (!player.hasPermission("jellycosmetics.namepaint")) {
                    player.sendMessage("§cYou don't have permission to use name paints.");
                    return true;
                }

                // Open name paint selection GUI
                new NamePaintGUI(plugin, player).open();
                return true;

            case "viewcolors":
                if (!player.hasPermission("jellycosmetics.view")) {
                    player.sendMessage("§cYou don't have permission to view other players' cosmetics.");
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage("§cUsage: /cosmetic viewcolors <player>");
                    return true;
                }

                Player colorTarget = Bukkit.getPlayer(args[1]);
                if (colorTarget == null) {
                    player.sendMessage("§cPlayer not found.");
                    return true;
                }

                // Show target player's chat color and name paint
                String chatColorId = plugin.getDatabaseManager().getCosmeticRepository()
                        .getPlayerChatColor(colorTarget.getUniqueId());
                String namePaintId = plugin.getDatabaseManager().getCosmeticRepository()
                        .getPlayerNamePaint(colorTarget.getUniqueId());

                player.sendMessage("§6" + colorTarget.getName() + "'s Colors:");

                if (chatColorId != null) {
                    player.sendMessage("§7Chat Color: §r" +
                            plugin.getConfigManager().getChatColorById(chatColorId).getName());
                } else {
                    player.sendMessage("§7Chat Color: §fNone");
                }

                if (namePaintId != null) {
                    player.sendMessage("§7Name Paint: §r" +
                            plugin.getConfigManager().getNamePaintById(namePaintId).getName());
                } else {
                    player.sendMessage("§7Name Paint: §fNone");
                }

                return true;

            case "ah":
            case "auction":
                if (!player.hasPermission("jellycosmetics.auction")) {
                    player.sendMessage("§cYou don't have permission to use the auction house.");
                    return true;
                }

                // Open auction house GUI
                new AuctionHouseGUI(plugin, player).open();
                return true;

            case "help":
                sendHelpMessage(player);
                return true;

            default:
                player.sendMessage("§cUnknown subcommand. Use /cosmetic help for help.");
                return true;
        }
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage("§6§l=== JellyMC Cosmetics Help ===");
        player.sendMessage("§e/cosmetic §7- Open your cosmetic backpack");
        player.sendMessage("§e/cosmetic view <player> §7- View another player's cosmetics");
        player.sendMessage("§e/cosmetic chatcolor §7- Change your chat color");
        player.sendMessage("§e/cosmetic namepaint §7- Change your name paint");
        player.sendMessage("§e/cosmetic viewcolors <player> §7- View another player's colors");
        player.sendMessage("§e/cosmetic ah §7- Open the auction house");
        player.sendMessage("§e/cosmetic help §7- Show this help message");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        Player player = (Player) sender;

        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();

            if (player.hasPermission("jellycosmetics.use")) {
                subcommands.add("help");
            }

            if (player.hasPermission("jellycosmetics.view")) {
                subcommands.add("view");
                subcommands.add("viewcolors");
            }

            if (player.hasPermission("jellycosmetics.chatcolor") &&
                    plugin.getConfig().getBoolean("chat_colors.enabled", true)) {
                subcommands.add("chatcolor");
            }

            if (player.hasPermission("jellycosmetics.namepaint") &&
                    plugin.getConfig().getBoolean("name_paints.enabled", true)) {
                subcommands.add("namepaint");
            }

            if (player.hasPermission("jellycosmetics.auction")) {
                subcommands.add("ah");
                subcommands.add("auction");
            }

            return filterStartingWith(subcommands, args[0]);
        } else if (args.length == 2) {
            if ((args[0].equalsIgnoreCase("view") || args[0].equalsIgnoreCase("viewcolors")) &&
                    player.hasPermission("jellycosmetics.view")) {
                return getOnlinePlayerNames(args[1]);
            }
        }

        return new ArrayList<>();
    }

    private List<String> filterStartingWith(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getOnlinePlayerNames(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
