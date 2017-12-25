package org.steveshipway.dynmap;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandMcd implements CommandExecutor {

    // This method is called, when somebody uses our command
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.sendMessage(ChatColor.RED + "This command may be run from the console only.");
            return false;
        }
        if ( args.length < 1 ) {
        	sender.sendMessage("Invalid number of arguments.  Try /mcd reload");
        	return true;
        }
        if( args[0].toLowerCase().startsWith("reload")) {
        	sender.sendMessage("Reloading all worlds due to command...");
        	DynmapMCDungeon thisplugin = (DynmapMCDungeon)(JavaPlugin.getPlugin(DynmapMCDungeon.class));
        	thisplugin.activate(); // this will cause a reload of the data 
        	return true;
        }
    	sender.sendMessage("Invalid subcommand.  Try /mcd reload");
        return true;
    }
}
