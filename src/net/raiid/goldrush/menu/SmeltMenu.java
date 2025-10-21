package net.raiid.goldrush.menu;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import net.raiid.goldrush.GoldRushItem;
import net.raiid.util.Item;
import net.raiid.util.TextUtil;

public class SmeltMenu implements Listener {

	private final JavaPlugin plugin;
	private final Map<UUID, Double> firePowers = new HashMap<>();
	private final Map<UUID, BukkitTask> smeltingTasks = new HashMap<>();

	private static final double MAX_FIRE_POWER = 100.0;//火力の最大値
	private static final double CLICK_POWER_INCREASE = 7.5;//1クリックあたりの火力上昇量
	private static final double FIRE_DECAY_PER_TICK = 2.5;//1ティックあたりの火力減少量
	private static final double INITIAL_FIRE_POWER = 10.0;//精錬開始時の初期火力

	public SmeltMenu(JavaPlugin plugin) {
		this.plugin = plugin;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	public static void openMenu(Player player) {
		FurnaceInventory inv = (FurnaceInventory)Bukkit.createInventory(null, InventoryType.FURNACE, "金を精錬する");
		ItemStack trigger = Item.create(Material.CAMPFIRE).name(TextUtil.color("&a&l精錬開始")).lore(TextUtil.color("&7上に精錬するアイテムを入れて、ここをクリックして確定"), TextUtil.color("&7精錬中にクリックすると火力が上がります")).getItemStack();
		inv.setFuel(trigger);
		player.openInventory(inv);
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (event.getClickedBlock() == null) return;
		Block block = event.getClickedBlock();
		if (block.getType() != Material.BLAST_FURNACE) return;
		Player player = event.getPlayer();
		event.setCancelled(true);
		openMenu(player);
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) return;
	    if (event.getInventory().getType() != InventoryType.FURNACE) return;
	    if (event.getView().getTitle() == null) return;
	    if (!event.getView().getTitle().equals("金を精錬する")) return;
	    Player player = (Player) event.getWhoClicked();
	    FurnaceInventory inv = (FurnaceInventory) event.getInventory();
	    if (event.getClickedInventory() == event.getView().getBottomInventory()) {
	    	if (!event.isShiftClick()) return;
	        if (event.getClick() == ClickType.SHIFT_RIGHT) return;
	        ItemStack clickedItem = event.getCurrentItem();
	        if (clickedItem == null || clickedItem.getType().isAir()) return;
	        if (inv.getSmelting() != null) return;
	        event.setCancelled(true);
	        inv.setSmelting(clickedItem);
	        event.setCurrentItem(null);
	    } else {
			if (this.smeltingTasks.containsKey(player.getUniqueId())) {//精錬中のクリック
			    event.setCancelled(true);
			    if (event.getRawSlot() != 1) return;//燃料スロットかどうか
				double currentPower = firePowers.getOrDefault(player.getUniqueId(), 0.0);
				double newPower = Math.min(MAX_FIRE_POWER, currentPower + CLICK_POWER_INCREASE);
				firePowers.put(player.getUniqueId(), newPower);
				player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, SoundCategory.MASTER, 0.4f, 0.5f + (float)(newPower / MAX_FIRE_POWER));//火力に応じて音程を変える
				return;
			} else {
			    if (event.getRawSlot() != 1) return;//燃料スロットかどうか
			    event.setCancelled(true);
			    if (inv.getSmelting() == null) {
					return;
				}
			    ItemStack item = inv.getSmelting();
			    double weight = GoldRushItem.getGoldWeight(item);
			    if (weight == 0) {
			    	player.sendMessage(TextUtil.color("&c&lこれは精錬対象ではありません"));
			    	return;
			    }
				ItemStack trigger = Item.create(Material.CAMPFIRE).name(TextUtil.color("&c&l精錬中")).lore(TextUtil.color("&7クリックすると火力が上がります")).getItemStack();
				inv.setFuel(trigger);
				player.playSound(player.getLocation(), Sound.BLOCK_FURNACE_FIRE_CRACKLE, SoundCategory.MASTER, 1.0f, 1.0f);
				final int DURATION_TICKS = 100; // 5秒 (5 * 20 ticks)
				BukkitTask task = new BukkitRunnable() {
					private int ticks = 0;
					private double totalFirePower = 0;//平均火力計算用

					@Override
					public void run() {
						if (!player.isOnline() || !player.getOpenInventory().getTitle().equals("金を精錬する")) {
							this.cancel();
							return;
						}
						this.ticks++;
						double currentPower = firePowers.getOrDefault(player.getUniqueId(), 0.0);//火力を取得し、自然減少させる
						double newPower = Math.max(0.0, currentPower - FIRE_DECAY_PER_TICK);
						firePowers.put(player.getUniqueId(), newPower);
						this.totalFirePower += newPower;//平均火力のために加算

						InventoryView view = player.getOpenInventory();
						view.setProperty(InventoryView.Property.BURN_TIME, (int) (newPower * 2.0));
						int cookProgress = (int) (((double) this.ticks / DURATION_TICKS) * 200.0);
						view.setProperty(InventoryView.Property.COOK_TIME, cookProgress);
						if (this.ticks >= DURATION_TICKS)
							this.cancel();
					}

					@Override
					public synchronized void cancel() throws IllegalStateException {
						super.cancel();
						smeltingTasks.remove(player.getUniqueId());
						firePowers.remove(player.getUniqueId());
	
						double averageFirePower = totalFirePower / DURATION_TICKS;
						player.sendMessage(TextUtil.color("&a&l精錬完了！"));
						//player.sendMessage(TextUtil.color("&7(平均火力: " + String.format("%.1f", averageFirePower) + ")"));//デバッグ用
						player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, SoundCategory.MASTER, 1.0f, 1.2f);
						
						if (player.getOpenInventory().getTitle().equals("金を精錬する")) {
							FurnaceInventory currentInv = (FurnaceInventory) player.getOpenInventory().getTopInventory();
							ItemStack trigger = Item.create(Material.CAMPFIRE).name(TextUtil.color("&a&l精錬開始")).lore(TextUtil.color("&7上に精錬するアイテムを入れて、ここをクリックして確定"), TextUtil.color("&7精錬中にクリックすると火力が上がります")).getItemStack();
							inv.setFuel(trigger);

						    double weight = GoldRushItem.getGoldWeight(item);
							double impurities = GoldRushItem.getImpurities(item);

							final double optimalFirePower = MAX_FIRE_POWER * 0.675; //理論値火力(80.0)
							if (averageFirePower > optimalFirePower) {//火力が理論値を超えた場合: 金を少しずつ損失する
								//理論値をどれだけ超えたか度合いを計算(0.0~20.0)
								double excessPower = averageFirePower - optimalFirePower;
								double goldLossRate = (excessPower / (MAX_FIRE_POWER - optimalFirePower));//超過度合いを 0.0 ~ 1.0 に正規化
								goldLossRate *= 0.05; //超過度合いに応じて金の損失率を決定(最大火力100の時、最大5%の金を失う)
								double goldToLose = weight * goldLossRate;
								weight -= goldToLose;
								//player.sendMessage(TextUtil.color(String.format("&c火力が強すぎたため、金が %.2fg 蒸発しました。", goldToLose)));//デバッグ用
							} else {//火力が理論値以下の場合: 不純物を減少させる
								double efficiency = averageFirePower / optimalFirePower;//理論値に対してどれくらいの火力効率だったかを計算(0.0~1.0の範囲)
								double impurityReductionRate = efficiency * 0.50;//火力効率に応じて不純物の削減率を決定する(理論値ぴったりで最大50%の不純物を除去できると仮定)
								double impuritiesToRemove = impurities * impurityReductionRate;
								impurities -= impuritiesToRemove;
								//player.sendMessage(TextUtil.color(String.format("&a精錬により、不純物を %.2fg 除去しました。", impuritiesToRemove)));//デバッグ用
							}
							currentInv.setSmelting(null);
							ItemStack result = GoldRushItem.INGOT_2;
							if (currentInv.getResult() != null && currentInv.getResult().getType() != Material.AIR) {
								result = currentInv.getResult();
								weight += GoldRushItem.getGoldWeight(result);
								impurities += GoldRushItem.getImpurities(result);
							}
							double heavy = weight + impurities;
							if (heavy >= 10000.0) result = GoldRushItem.INGOT_7;
							else if (heavy >= 5000.0) result = GoldRushItem.INGOT_6;
							else if (heavy >= 1000.0) result = GoldRushItem.INGOT_5;
							else if (heavy >= 500.0) result = GoldRushItem.INGOT_4;
							else if (heavy >= 250.0) result = GoldRushItem.INGOT_3;
							else if (heavy >= 100.0) result = GoldRushItem.INGOT_2;
							
							currentInv.setResult(GoldRushItem.setGold(result, weight, impurities, false, false));
							
							player.getOpenInventory().setProperty(InventoryView.Property.BURN_TIME, 0);
							player.getOpenInventory().setProperty(InventoryView.Property.COOK_TIME, 0);
						}
					}
				}.runTaskTimer(this.plugin, 0L, 1L);
				this.firePowers.put(player.getUniqueId(), INITIAL_FIRE_POWER);
				this.smeltingTasks.put(player.getUniqueId(), task);
			}
	    }
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player)) return;
		Player player = (Player)event.getPlayer();
	    if (event.getInventory().getType() != InventoryType.FURNACE) return;
	    if (event.getView().getTitle() == null) return;
	    if (!event.getView().getTitle().equals("金を精錬する")) return;
		BukkitTask task = this.smeltingTasks.remove(player.getUniqueId());
		if (task != null) {
			task.cancel();
		}
		this.firePowers.remove(player.getUniqueId());
	    FurnaceInventory inv = (FurnaceInventory)event.getInventory();
	    if (inv.getSmelting() != null)
	    	player.getWorld().dropItem(player.getLocation(), inv.getSmelting());
	    if (inv.getResult() != null)
	    	player.getWorld().dropItem(player.getLocation(), inv.getResult());
	}

}