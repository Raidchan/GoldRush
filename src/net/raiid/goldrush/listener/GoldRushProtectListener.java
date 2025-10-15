package net.raiid.goldrush.listener;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class GoldRushProtectListener implements Listener {

	public GoldRushProtectListener(JavaPlugin plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onCraft(CraftItemEvent event) {
		event.setCancelled(true);
	}

	@EventHandler
	public void onClick(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Block block = event.getClickedBlock();
        if (block == null) return;
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (block.getType().toString().contains("_DOOR"))
            event.setCancelled(true);
        if (block.getType().toString().contains("_TRAPDOOR"))
            event.setCancelled(true);
        if (block.getType().toString().contains("_FENCE_GATE"))
            event.setCancelled(true);
        if (block.getType().toString().contains("_SIGN"))
            event.setCancelled(true);
        if (block.getType().toString().contains("_BED"))
            event.setCancelled(true);
        if (block.getType().toString().contains("_CANDLE"))
            event.setCancelled(true);
        if (block.getType().toString().contains("CAMPFIRE"))
            event.setCancelled(true);
        if (block.getType().toString().contains("ANVIL"))
            event.setCancelled(true);
	}

}
