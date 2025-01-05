package com.dalvi;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SetMaxDistanceCommand implements CommandExecutor {

    private final WebVoiceChatPlugin plugin;

    public SetMaxDistanceCommand(WebVoiceChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /setmaxdistance <distance>");
            return true;
        }

        try {
            // Parse the distance
            double distance = Double.parseDouble(args[0]);
            if (distance <= 0) {
                sender.sendMessage("§cThe distance must be greater than 0!");
                return true;
            }

            WebVoiceChatPlugin.setMaxDistance(distance);

            sender.sendMessage("§aThe maximum distance has been updated to " + distance + " blocks.");

            plugin.getServer().broadcastMessage("§eMaximum voice distance updated to " + distance + " blocks.");

        } catch (NumberFormatException e) {
            sender.sendMessage("§cThe entered value must be a valid number!");
        }

        return true;
    }
}
