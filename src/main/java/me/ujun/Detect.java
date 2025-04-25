package me.ujun;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.ServerCommandEvent;


import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Detect implements Listener {
    private Main plugin;

    public Detect(Main plugin) {
        this.plugin = plugin;
    }

    //When user use command
    @EventHandler(priority= EventPriority.LOWEST, ignoreCancelled=true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().toLowerCase();

        //reload ops.json
        if (event.getMessage().startsWith("/op") || event.getMessage().startsWith("/deop")) {
            plugin.reloadOpsCache();
        }

        if (player.isOp()) {
            String baseCommand = message.substring(1);
            String[] split = baseCommand.split(" ");

            String primaryCommand = split[0];


            String actualCommand = primaryCommand;
            if (primaryCommand.equals("execute")) {

                for (int i = 1; i < split.length - 1; i++) {
                    if (split[i].equals("run")) {
                        actualCommand = split[i + 1];
                        break;
                    }
                }
            }


            int localOpLevel = plugin.perPlayerLevel.getOrDefault(player.getUniqueId().toString(),plugin.opLevel);
            List<String> disabledOpCommands = plugin.disabledCommandCache.getOrDefault(localOpLevel, Collections.emptyList());


            if (disabledOpCommands.contains(actualCommand.toLowerCase())) {
                player.sendMessage(ChatColor.RED + plugin.getConfig().getString("disallowMessage"));
                event.setCancelled(true);
            }
        }
    }

    //tabcompleter
    @EventHandler(priority= EventPriority.LOWEST, ignoreCancelled=true)
    public void onCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();

        if (player.isOp()) {

            int localOpLevel = plugin.perPlayerLevel.getOrDefault(player.getUniqueId().toString(),plugin.opLevel);
            List<String> disabledOpCommands = plugin.disabledCommandCache.getOrDefault(localOpLevel, Collections.emptyList());


            for (String command : disabledOpCommands) {
                event.getCommands().remove(command);
            }
        }
    }

    //When console use op or deop
    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void redirectConsoleCommand(ServerCommandEvent event) {
        if (event.getCommand().startsWith("op") || event.getCommand().startsWith("deop")) {
            plugin.reloadOpsCache();
        }
    }

    //Command block interact
    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Integer localOpLevel = (plugin.perPlayerLevel.containsKey(event.getPlayer().getUniqueId().toString()) ? plugin.perPlayerLevel.get(event.getPlayer().getUniqueId().toString()) : plugin.opLevel);
        Material clickedBlock = event.getClickedBlock().getType();

        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && (clickedBlock.equals(Material.COMMAND_BLOCK)) || (clickedBlock.equals(Material.REPEATING_COMMAND_BLOCK)) || (clickedBlock.equals(Material.CHAIN_COMMAND_BLOCK))) {
            if (localOpLevel < 2) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + plugin.getConfig().getString("disallowMessage"));
                event.getPlayer().closeInventory();
            }
        }
    }

    //commandblock
    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void onServerCommand(ServerCommandEvent event) {
        CommandSender sender = event.getSender();
        String rawCommand = event.getCommand().toLowerCase();


        if (!(sender instanceof BlockCommandSender)) return;

        String actualCommand = rawCommand.split(" ")[0];


        if (actualCommand.equals("execute")) {
            String[] parts = rawCommand.split(" ");
            for (int i = 0; i < parts.length - 1; i++) {
                if (parts[i].equals("run")) {
                    actualCommand = parts[i + 1];
                    break;
                }
            }
        }


        List<String> disabledOpCommands = plugin.disabledCommandCache.getOrDefault(2 , Collections.emptyList());

        if (disabledOpCommands.contains(actualCommand)) {
            sender.sendMessage(ChatColor.RED + plugin.getConfig().getString("disallowMessage"));
            event.setCommand("");
        }
    }



}
