package me.ujun;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import javax.swing.plaf.basic.BasicButtonUI;

public class Command implements CommandExecutor {
    private Main plugin;

    public Command(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {

        if(command.getName().equals("reload-bukkitopfix")) {
            if (args.length == 1) {

            String subCommand = args[0];

            switch (subCommand) {
                case "config":
                   plugin.reloadConfig();
                   plugin.loadCommandRestrictions();
                   sender.sendMessage(ChatColor.GREEN + plugin.getConfig().getString("reloadPluginMessage"));
                   break;
                case "ops":
                    plugin.reloadOpsCache();
                    sender.sendMessage(ChatColor.GREEN + plugin.getConfig().getString("reloadOpsMessage"));
                    break;
                default:
                    return false;
                }
            } else {
                return false;
            }
        } else if (command.getName().equals("setoplevel")) {
            OfflinePlayer p = Bukkit.getOfflinePlayer(args[0]);
            int newLevel = Integer.parseInt(args[1]);
            newLevel = Math.max(1, newLevel);
            newLevel = Math.min(newLevel, 4);

            if (!p.isOp()) {
                sender.sendMessage(ChatColor.RED + "That player is not op");
                return false;
            }

            if (plugin.perPlayerLevel.get(p.getUniqueId().toString()).equals(newLevel)) {
                sender.sendMessage(ChatColor.RED + "Same as the previous level");
                return false;
            }

            plugin.perPlayerLevel.put(p.getUniqueId().toString(), newLevel);
            plugin.updateOpsJson();
            sender.sendMessage( String.format("set %s's op level to %d", p.getName(), newLevel));
        }

        return false;
    }
}
