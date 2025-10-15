package net.raiid.customblock;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomBlockCommand implements CommandExecutor {

	public CustomBlockCommand(JavaPlugin plugin) {
		plugin.getCommand("customblock").setExecutor(this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String arg0, String[] args) {
		if (!(sender instanceof Player)) return true;
		Player player = (Player)sender;
		for (CustomBlock cb : CustomBlockListener.allCustomBlocks) {
			player.getInventory().addItem(cb.getItemStack());
		}
		return false;
	}

}
