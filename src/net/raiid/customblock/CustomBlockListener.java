package net.raiid.customblock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.raiid.customblock.blocks.GoldenDirt;
import net.raiid.goldrush.Main;

public class CustomBlockListener implements Listener {

	public static List<CustomBlock> allCustomBlocks = new ArrayList<>();
	public static Map<Player, Location> target = new HashMap<>();
	public static Map<Player, CustomBlockHandler> listen = new HashMap<>();

	public static Map<Location, CustomBlock> placed = new HashMap<>();

	public CustomBlockListener(JavaPlugin plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
		new CustomBlockCommand(plugin);
		register(new GoldenDirt());
	}

	public static void register(CustomBlock cb) {
		allCustomBlocks.add(cb);
	}

	public void setSlowDig(Player player, boolean flag) {
		if (flag)
			player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 3, 3, false, false));
		else
        	player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
	}

	public CustomBlock isCustomBlock(Location loc) {
		for (Entry<Location, CustomBlock> entry : placed.entrySet()) {
			if (loc.distance(entry.getKey()) == 0) {
				return entry.getValue();
			}
		}
		return null;
	}

	@EventHandler
	public void onPlace(BlockPlaceEvent event) {
		ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
		if (event.getHand() == EquipmentSlot.OFF_HAND)
			item = event.getPlayer().getInventory().getItemInOffHand();
		for (CustomBlock cb : allCustomBlocks) {
			if (cb.placeBlock(event.getBlock().getLocation(), item, event.getPlayer())) {
				placed.put(event.getBlock().getLocation(), cb);
			}
		}
	}

	@EventHandler
	public void onBreak(BlockBreakEvent event) {
		if (placed.containsKey(event.getBlock().getLocation()))
			if (event.getPlayer().getGameMode() == GameMode.CREATIVE)
				placed.remove(event.getBlock().getLocation());
			else
				event.setCancelled(true);
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (event.getClickedBlock() == null) return;
		if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
		Player player = event.getPlayer();
		if (player.getGameMode() == GameMode.CREATIVE) return;
		Populartor.patch(event.getClickedBlock().getLocation());
		CustomBlock cb = this.isCustomBlock(event.getClickedBlock().getLocation());
		if (cb != null) {
			this.setSlowDig(player, true);
			if (player.getGameMode() == GameMode.SURVIVAL) {
				CustomBlockHandler service;
				if (listen.containsKey(player))
					service = listen.get(player);
				else
					service = new CustomBlockHandler(player);
				service.setCustomBlock(cb);
				service.setLocation(event.getClickedBlock().getLocation());
				listen.put(player, service);
				target.put(player, event.getClickedBlock().getLocation());
				return;
			} else if (player.getGameMode() == GameMode.ADVENTURE) {
				if (target.containsKey(player) && target.get(player).distance(event.getClickedBlock().getLocation()) == 0) {
			    	Location loc = target.get(player);
			    	CustomBlockHandler service = listen.get(player);
			    	service.getCustomBlock().damageBlock(loc, service.getDamage(), player);
					service.damage();
			    } else {
					CustomBlockHandler service;
					if (listen.containsKey(player))
						service = listen.get(player);
					else
						service = new CustomBlockHandler(player);
					service.setCustomBlock(cb);
					service.setLocation(event.getClickedBlock().getLocation());
					listen.put(player, service);
					target.put(player, event.getClickedBlock().getLocation());
			    }
			}
		} else {
			if (target.containsKey(player)) {
				if (Arrays.asList(new Material[] {Material.AIR,Material.CAVE_AIR,Material.VOID_AIR}).contains(target.get(player).getBlock().getType())) {
					//placed.remove(target.get(player));
				}
			}
			listen.remove(player);
			target.remove(player);
			this.setSlowDig(player, false);
		}
	}

	@EventHandler
	public void onPlayerAnimation(PlayerAnimationEvent event) {
	    Player player = event.getPlayer();
	    if (player.getGameMode() == GameMode.SURVIVAL) {
		    if (listen.containsKey(player) && target.containsKey(player)) {
				this.setSlowDig(player, true);
		    	Location loc = target.get(player);
		    	CustomBlockHandler service = listen.get(player);
		    	service.getCustomBlock().damageBlock(loc, service.getDamage(), player);
		    	if (service.getLocation().distance(loc) == 0) {
					service.damage();
		    	}
		    } else {
				this.setSlowDig(player, false);
		    }
	    }
	}

}

class CustomBlockHandler {

	private CustomBlock cb = null;
	private Player player;
	private Location loc;
	private int damage = 0;

	public CustomBlockHandler(Player player) {
		this.player = player;
	}

	public void setCustomBlock(CustomBlock cb) {
		this.cb = cb;
		this.damage = 0;
	}
	public CustomBlock getCustomBlock() {
		return this.cb;
	}

	public Player getPlayer() {
		return this.player;
	}

	public void setDamage(int damage) {
		this.damage = damage;
	}
	public float getDamage() {
		return this.damage;
	}

	public void setLocation(Location loc) {
		this.loc = loc;
		this.damage = 0;
	}
	public Location getLocation() {
		return this.loc;
	}

	public void damage() {
		if (this.cb.getDurability() <= this.damage) return;
		this.damage++;
		double bkDmg = this.damage;
		this.cb.damageBlock(this.loc, this.damage, this.player);
		if (this.cb.getDurability() <= this.damage) {
			this.damage = 0;
			this.cb.breakBlock(this.loc, this.player);
		} else {
			Bukkit.getScheduler().runTaskLater(Main.instance, () -> {
				if (this.damage == bkDmg)
					Bukkit.getOnlinePlayers().forEach(target -> {
						this.cb.sendBlockBreakAnimation(this.loc, -1, target);
					});
			}, 3);
		}
	}

}
