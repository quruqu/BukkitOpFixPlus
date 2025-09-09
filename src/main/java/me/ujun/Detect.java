package me.ujun;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.ServerCommandEvent;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class Detect implements Listener {
    private final Main plugin;

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

            int localOpLevel = plugin.perPlayerLevel.getOrDefault(player.getUniqueId().toString(),plugin.opLevel);

            if (isDisabledCommand(baseCommand, localOpLevel, 10)) {
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

    //commandblock, commandminecart
    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void onServerCommand(ServerCommandEvent event) {
        CommandSender sender = event.getSender();
        String rawCommand = event.getCommand().toLowerCase();


        if (!((sender instanceof BlockCommandSender) || (sender instanceof CommandMinecart))) return;

       if (isDisabledCommand(rawCommand, 2, 10)) {
           sender.sendMessage(ChatColor.RED + plugin.getConfig().getString("disallowMessage"));
           event.setCommand("");
       }


    }


    private boolean isDisabledCommand(String baseCommand, int opLevel, int depth) {
        depth--;

        if (depth == 0) {
            Bukkit.getLogger().info("너무 깊음");
            return true;
        }

        String[] split = baseCommand.split(" ");
        String actualCommand = split[0];
        List<String> disabledOpCommands = plugin.disabledCommandCache.getOrDefault(opLevel , Collections.emptyList());

        if (actualCommand.equals("minecraft:execute") || actualCommand.equals("execute")) {
            for (int i = 0; i < split.length - 1; i++) {
                if (split[i].equals("run") && (split[i+1].equals("npc") || split[i+1].equals("fancynpcs:npc"))) {
//                    Bukkit.getLogger().info("익큣을 npc 명령어로 쓰고 있음");
                    actualCommand = "npc";
                    break;
                }

            }
        }

        if (actualCommand.equals("npc") || actualCommand.equals("fancynpcs:npc")) {
//            Bukkit.getLogger().info("npc인 건 감지함");
            if (baseCommand.contains("action") && baseCommand.contains("console_command")) {
//                Bukkit.getLogger().info("action이랑 console_command 있는 거 감지함");
                for (int i = 1; i < split.length - 1; i++) {
                    if (split[i].equals("console_command") && disabledOpCommands.contains(split[i + 1])) {
//                        Bukkit.getLogger().info("consolecommand + " + split[i + 1]);
                        return true;
                    } else if (split[i].equals("console_command") && (split[i+1].equals("execute") || (split[i+1].equals("minecraft:execute")))) {
//                        Bukkit.getLogger().info("console_command에 execute 발견");
                        actualCommand = "execute";
                        break;
                    }
//                    else if (split[i].equals("console_command") && (split[i+1].equals("npc") || (split[i+1].equals("fancynpcs:npc")))) {
//                        String result = String.join(" ", Arrays.copyOfRange(split, i, split.length));
//                        return isDisabledCommand(result, opLevel, depth);
//                    }

                }
            }
        }

        //execute run execute store result ditto.storage console_command int 1 run npc action pr2n set 1 console_command kick

        if (actualCommand.equals("minecraft:execute") || actualCommand.equals("execute")) {
            for (int i = 0; i < split.length - 1; i++) {
                if (split[i].equals("run") && disabledOpCommands.contains(split[i + 1])) {
                    return true;
                }
            }
        }


        return disabledOpCommands.contains(actualCommand);
    }

}
