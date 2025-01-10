package com.dalvi.auth;

import com.dalvi.WebVoiceChatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AuthCommand  implements CommandExecutor {

    private final WebVoiceChatPlugin plugin;
    private AuthService authService;


    public AuthCommand(WebVoiceChatPlugin plugin, AuthService authService) {
        this.plugin = plugin;
        this.authService = authService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("auth")) return false;
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("on")) {
                plugin.setAuthRequired(true);
                sender.sendMessage("Authentication enabled.");
            } else if (args[0].equalsIgnoreCase("off")) {
                plugin.setAuthRequired(false);
                sender.sendMessage("Authentication disabled.");
            } else if (args[0].equalsIgnoreCase("status")) {
                sender.sendMessage("Authentication is " + (plugin.isAuthRequired() ? "enabled" : "disabled"));
            } else {
                sender.sendMessage("Usage: /auth <on|off|status>");
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (!plugin.isAuthRequired()) {
            sender.sendMessage("Authentication is disabled.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        String code = authService.generateAuthCode(playerId);
        Bukkit.getScheduler().runTaskLater(plugin, () -> authService.removeAuthCode(playerId), 300 * 20);
        player.sendMessage("Your authentication code: " + code);
        player.sendMessage("Valid for 5 minutes.");
        return true;
    }
}