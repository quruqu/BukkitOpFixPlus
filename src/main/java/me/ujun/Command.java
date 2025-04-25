package me.ujun;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

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
        }

        return false;
    }
}
