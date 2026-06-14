package com.jellymc.cosmetics.spigot.command;

import com.jellymc.cosmetics.core.model.Cosmetic;
import com.jellymc.cosmetics.spigot.JellyCosmeticsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CosmeticAdminCommand implements CommandExecutor, TabCompleter {

    private final JellyCosmeticsPlugin plugin;

    public CosmeticAdminCommand(JellyCosmeticsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jellycosmetics.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendAdminHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /cosmeticadmin give <player> <cosmetic_id>");
                    return true;
                }

                // Get target player
                String playerName = args[1];
                Player target = Bukkit.getPlayer(playerName);
                UUID targetUuid;

                if (target == null) {
                    // Try to get UUID from offline player
                    targetUuid = getOfflinePlayerUuid(playerName);
                    if (targetUuid == null) {
                        sender.sendMessage("§cPlayer not found.");
                        return true;
                    }
                } else {
                    targetUuid = target.getUniqueId();
                }

                // Get cosmetic
                String cosmeticId = args[2];
                Cosmetic cosmetic = plugin.getConfigManager().getCosmeticById(cosmeticId);

                if (cosmetic == null) {
                    sender.sendMessage("§cCosmetic not found: " + cosmeticId);
                    return true;
                }

                // Give cosmetic to player
                plugin.getDatabaseManager().getCosmeticRepository().giveCosmetic(targetUuid, cosmeticId);

                // Notify player if online
                if (target != null) {
                    target.sendMessage("§aYou received the cosmetic: §e" + cosmetic.name());
                }

                // Notify admin
                sender.sendMessage("§aGave cosmetic §e" + cosmetic.name() + " §ato player §e" + playerName);

                // Notify other servers
                plugin.getSyncManager().notifyCosmeticGiven(targetUuid, cosmeticId);

                return true;

            case "remove":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /cosmeticadmin remove <player> <cosmetic_id>");
                    return true;
                }

                // Get target player
                String removePlayerName = args[1];
                Player removeTarget = Bukkit.getPlayer(removePlayerName);
                UUID removeTargetUuid;

                if (removeTarget == null) {
                    // Try to get UUID from offline player
                    removeTargetUuid = getOfflinePlayerUuid(removePlayerName);
                    if (removeTargetUuid == null) {
                        sender.sendMessage("§cPlayer not found.");
                        return true;
                    }
                } else {
                    removeTargetUuid = removeTarget.getUniqueId();
                }

                // Get cosmetic
                String removeCosmeticId = args[2];
                Cosmetic removeCosmetic = plugin.getConfigManager().getCosmeticById(removeCosmeticId);

                if (removeCosmetic == null) {
                    sender.sendMessage("§cCosmetic not found: " + removeCosmeticId);
                    return true;
                }

                // Remove cosmetic from player
                plugin.getDatabaseManager().getCosmeticRepository().removeCosmetic(removeTargetUuid, removeCosmeticId);

                // Notify player if online
                if (removeTarget != null) {
                    removeTarget.sendMessage("§cThe cosmetic §e" + removeCosmetic.name() + " §chas been removed from your account.");
                }

                // Notify admin
                sender.sendMessage("§aRemoved cosmetic §e" + removeCosmetic.name() + " §afrom player §e" + removePlayerName);

                return true;

            case "list":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /cosmeticadmin list <player>");
                    return true;
                }

                // Get target player
                String listPlayerName = args[1];
                Player listTarget = Bukkit.getPlayer(listPlayerName);
                UUID listTargetUuid;

                if (listTarget == null) {
                    // Try to get UUID from offline player
                    listTargetUuid = getOfflinePlayerUuid(listPlayerName);
                    if (listTargetUuid == null) {
                        sender.sendMessage("§cPlayer not found.");
                        return true;
                    }
                } else {
                    listTargetUuid = listTarget.getUniqueId();
                }

                // Get player's cosmetics
                List<String> cosmeticIds = plugin.getDatabaseManager().getCosmeticRepository()
                        .getPlayerCosmeticIds(listTargetUuid);

                if (cosmeticIds.isEmpty()) {
                    sender.sendMessage("§e" + listPlayerName + " §cdoesn't have any cosmetics.");
                    return true;
                }

                sender.sendMessage("§6§l" + listPlayerName + "'s Cosmetics:");

                for (String id : cosmeticIds) {
                    Cosmetic c = plugin.getConfigManager().getCosmeticById(id);
                    if (c != null) {
                        boolean equipped = plugin.getDatabaseManager().getCosmeticRepository()
                                .isEquipped(listTargetUuid, id);

                        sender.sendMessage("§8- " + (equipped ? "§a[E] " : "§7") +
                                c.getRarityColor() + c.name() + " §8(" + id + ")");
                    }
                }

                return true;

            case "reload":
                // Reload configuration
                plugin.getConfigManager().reloadConfigs();
                sender.sendMessage("§aCosmetics configuration reloaded!");
                return true;

            case "logs":
                sender.sendMessage("§cLogs feature not yet implemented.");
                return true;

            case "help":
                sendAdminHelpMessage(sender);
                return true;

            default:
                sender.sendMessage("§cUnknown subcommand. Use /cosmeticadmin help for help.");
                return true;
        }
    }

    @SuppressWarnings("deprecation")
    private UUID getOfflinePlayerUuid(String playerName) {
        // Try to get UUID from offline player
        return Bukkit.getOfflinePlayer(playerName).getUniqueId();
    }

    private void sendAdminHelpMessage(CommandSender sender) {
        sender.sendMessage("§6§l=== JellyMC Cosmetics Admin Help ===");
        sender.sendMessage("§e/cosmeticadmin give <player> <cosmetic_id> §7- Give a cosmetic to a player");
        sender.sendMessage("§e/cosmeticadmin remove <player> <cosmetic_id> §7- Remove a cosmetic from a player");
        sender.sendMessage("§e/cosmeticadmin list <player> §7- List a player's cosmetics");
        sender.sendMessage("§e/cosmeticadmin reload §7- Reload configuration");
        sender.sendMessage("§e/cosmeticadmin logs §7- View recent logs");
        sender.sendMessage("§e/cosmeticadmin help §7- Show this help message");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("jellycosmetics.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            subcommands.add("give");
            subcommands.add("remove");
            subcommands.add("list");
            subcommands.add("reload");
            subcommands.add("logs");
            subcommands.add("help");

            return filterStartingWith(subcommands, args[0]);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") ||
                    args[0].equalsIgnoreCase("remove") ||
                    args[0].equalsIgnoreCase("list")) {
                return getOnlinePlayerNames(args[1]);
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("remove")) {
                return getCosmeticIds(args[2]);
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

    private List<String> getCosmeticIds(String prefix) {
        return plugin.getConfigManager().getAllCosmetics().stream()
                .map(Cosmetic::id)
                .filter(id -> id.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
