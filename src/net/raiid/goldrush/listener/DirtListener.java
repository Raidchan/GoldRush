package net.raiid.goldrush.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.raiid.goldrush.DirtBundle;
import net.raiid.goldrush.GoldRushItem;
import net.raiid.goldrush.Main;

public class DirtListener implements Listener {

	public DirtListener(JavaPlugin plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onDirtClick(PlayerInteractEvent event) {
	    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
	    if (event.getHand() != EquipmentSlot.HAND) return;
	    Player player = event.getPlayer();
	    Block block = event.getClickedBlock();
	    ItemStack item = player.getInventory().getItemInMainHand();
	    if (event.getHand() == EquipmentSlot.OFF_HAND)
	    	item = player.getInventory().getItemInOffHand();
	    if (!DirtBundle.isDirtBundle(item)) return;
	    if (block.getType() != Material.CLAY) return;
	    event.setCancelled(true);
	    double currentWeight = DirtBundle.getDirtWeight(item);
	    if (currentWeight >= DirtBundle.getMaxWeight(item)) {
	        player.sendMessage(ChatColor.RED + "バンドルが満タンです！");
	        return;
	    }
	    block.setType(Material.BEDROCK);
	    block.getWorld().playSound(block.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.0f);
	    DirtBundle.addDirt(item, 10, 10);
	}

	private Map<Player, Integer> panningPlayers = new HashMap<>();

	@EventHandler
	public void onBowlClick(PlayerInteractEvent event) {
	    if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
	    if (event.getHand() != EquipmentSlot.HAND) return;
	    Player player = event.getPlayer();

	    ItemStack main = player.getInventory().getItemInMainHand();
	    if (main == null) return;
	    if (main.getType() != Material.BOWL) return;
	    ItemStack off = player.getInventory().getItemInOffHand();
	    if (off == null) return;
	    if (!DirtBundle.isDirtBundle(off)) {
	        player.sendMessage(ChatColor.RED + "オフハンドに土砂バンドルを持ってください！");
	        return;
	    }
	    Block block = player.getLocation().getBlock();
	    if (block.getType() != Material.WATER) {
	        player.sendMessage(ChatColor.RED + "水中でパンニングしてください！");
	        return;
	    }
	    double dirtWeight = DirtBundle.getDirtWeight(off);
	    if (dirtWeight <= 0) {
	        player.sendMessage(ChatColor.RED + "バンドルが空です！");
	        return;
	    }
	    event.setCancelled(true);
	    if (this.panningPlayers.containsKey(player)) {
	        int clicks = this.panningPlayers.get(player);
	        this.panningPlayers.put(player, clicks++);
	        player.spawnParticle(Particle.SPLASH, player.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.05);
	        player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 0.3f, 1.5f);
	        return;
	    }
	    this.startPanning(player, off);
	}

	private void startPanning(Player player, ItemStack bundle) {
		this.panningPlayers.put(player, 1);
	    player.spawnParticle(Particle.SPLASH, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
	    player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1.0f, 1.0f);

	    Bukkit.getScheduler().runTaskLater(Main.instance, () -> {
	        finishPanning(player);
	    }, 60L);//3秒

	    AtomicInteger countdown = new AtomicInteger(3);
	    BukkitRunnable countdownTask = new BukkitRunnable() {
	        @Override
	        public void run() {
	            int remaining = countdown.decrementAndGet();
	            if (remaining <= 0) {
	                cancel();
	                return;
	            }
	            if (panningPlayers.containsKey(player)) {
	                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GOLD + "" + ChatColor.BOLD + "パンニング中.."));
	            }
	        }
	    };
	    countdownTask.runTaskTimer(Main.instance, 20L, 20L);
	}

	private void finishPanning(Player player) {
	    if (player == null || !player.isOnline()) {
	        panningPlayers.remove(player);
	        return;
	    }
	    int clicks = this.panningPlayers.remove(player);
	    double impurityRate = this.calculateImpurityRate(clicks);
		ItemStack bundle = player.getInventory().getItemInOffHand();
	    double goldWeight = DirtBundle.getGoldWeight(bundle);
	    double impurities = goldWeight * (impurityRate / (1.0 - impurityRate));
	    ItemStack result = GoldRushItem.INGOT_1;
	    GoldRushItem.setGold(result, goldWeight, impurities, false, false);
    	player.getWorld().dropItem(player.getLocation(), result);
	    player.getInventory().setItemInOffHand(DirtBundle.clearBundle(bundle));
	    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
	    player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "パンニング完了！");
	}

	private double calculateImpurityRate(int clicks) {
	    // クリック数による不純物率
	    // 基本的には不純物20-30%程度
	    // 大量にクリックしないと純度は大きく上がらない
	    if (clicks >= 50) {
	        // 50回以上: 超高純度 (不純物3-7%)
	        return 0.03 + (Math.random() * 0.04);
	    } else if (clicks >= 40) {
	        // 40-49回: 高純度 (不純物7-12%)
	        return 0.07 + (Math.random() * 0.05);
	    } else if (clicks >= 30) {
	        // 30-39回: 良好 (不純物12-17%)
	        return 0.12 + (Math.random() * 0.05);
	    } else if (clicks >= 20) {
	        // 20-29回: やや良い (不純物17-22%)
	        return 0.17 + (Math.random() * 0.05);
	    } else if (clicks >= 10) {
	        // 10-19回: 普通 (不純物22-27%)
	        return 0.22 + (Math.random() * 0.05);
	    } else if (clicks >= 1) {
	        // 1-9回: ほぼ変わらず (不純物25-30%)
	        return 0.25 + (Math.random() * 0.05);
	    } else {
	        // 0回: 基本 (不純物25-30%)
	        return 0.25 + (Math.random() * 0.05);
	    }
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
	    if (!(event.getWhoClicked() instanceof Player)) return;
	    Player player = (Player) event.getWhoClicked();
	    if (this.panningPlayers.containsKey(player))
	        event.setCancelled(true);
	}

	@EventHandler
	public void onInventoryDrag(InventoryDragEvent event) {
	    if (!(event.getWhoClicked() instanceof Player)) return;
	    Player player = (Player) event.getWhoClicked();
	    if (this.panningPlayers.containsKey(player))
	        event.setCancelled(true);
	}

	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent event) {
	    Player player = event.getPlayer();
	    if (this.panningPlayers.containsKey(player))
	        event.setCancelled(true);
	}

	@EventHandler
	public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
	    Player player = event.getPlayer();
	    if (panningPlayers.containsKey(player))
	        event.setCancelled(true);
	}

	@EventHandler
	public void onPlayerItemHeld(PlayerItemHeldEvent event) {
	    Player player = event.getPlayer();
	    if (this.panningPlayers.containsKey(player))
	        event.setCancelled(true);
	}

	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent event) {
	    if (!(event.getPlayer() instanceof Player)) return;
	    Player player = (Player) event.getPlayer();
	    if (this.panningPlayers.containsKey(player))
	        event.setCancelled(true);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (this.panningPlayers.containsKey(player))
			this.panningPlayers.remove(player);
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (this.panningPlayers.containsKey(player))
			this.panningPlayers.remove(player);
	}

}
