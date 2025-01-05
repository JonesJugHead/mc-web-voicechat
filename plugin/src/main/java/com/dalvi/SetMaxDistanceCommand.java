package com.dalvi;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
            // Parse la distance
            double distance = Double.parseDouble(args[0]);
            if (distance <= 0) {
                sender.sendMessage("§cLa distance doit être supérieure à 0 !");
                return true;
            }

            WebVoiceChatPlugin.setMaxDistance(distance);

            sender.sendMessage("§aLa distance maximale a été mise à jour à " + distance + " blocs.");

            plugin.getServer().broadcastMessage("§eDistance maximale de la voix mise à jour à " + distance + " blocs.");

        } catch (NumberFormatException e) {
            sender.sendMessage("§cLa valeur entrée doit être un nombre valide !");
        }

        return true;
    }
}
