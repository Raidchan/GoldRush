package net.raiid.goldrush.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import net.raiid.goldrush.GoldRushCore;
import net.raiid.goldrush.GoldRushCore.GoldType;
import net.raiid.goldrush.GoldRushCore.TradingPostType;
import net.raiid.goldrush.GoldRushItem;
import net.raiid.goldrush.PlayerDataManager;
import net.raiid.util.Item;
import net.raiid.util.TextUtil;

public class GoldSellMenu implements Listener {

	public GoldSellMenu(JavaPlugin plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	public static void openMenu(Player player, String name) {
		TradingPostType type = TradingPostType.nameOf(name);
		if (type == null) return;
		openMenu(player, type);
	}

	public static void openMenu(Player player, TradingPostType type) {
		Inventory inv = Bukkit.createInventory(null, InventoryType.HOPPER, type.getName() + "に金を売却する");
		ItemStack trigger = Item.create(Material.MINECART).name(TextUtil.color("&6&l売却を確定")).lore(TextUtil.color("&7左に売却するアイテムを入れて、ここをクリックして確定")).getItemStack();
		inv.setItem(4, trigger);
		player.openInventory(inv);
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) return;
	    if (event.getInventory().getType() != InventoryType.HOPPER) return;
	    if (event.getView().getTitle() == null) return;
	    if (!event.getView().getTitle().contains("に金を売却する")) return;
	    TradingPostType shoptype = TradingPostType.nameOf(event.getView().getTitle().replaceAll("に金を売却する", ""));
	    if (shoptype == null) return;
	    Inventory inv = event.getInventory();
	    if (event.getRawSlot() != 4) return;//確定スロットかどうか
	    event.setCancelled(true);
		Player player = (Player)event.getWhoClicked();
	    double weight = 0;
	    double price = 0;
	    for (int slot = 0; slot < 4; slot++) {
	    	ItemStack item = inv.getItem(slot);
	    	if (item == null) continue;
	    	double w = GoldRushItem.getGoldWeight(item);
	    	weight += w;
	    	GoldType type = GoldRushItem.getGoldType(item);
	    	if (type == null) continue;
	    	double p = w * GoldRushCore.getInstance().getSellPrice(shoptype, type);
	    	price += p;
	    	if (p == 0) continue;
	    	inv.setItem(slot, null);
	    }
	    if (price <= 0) return;
	    PlayerDataManager.addMoney(player, price);
	    player.closeInventory();
	    player.sendMessage(TextUtil.color("&6&l━━━━━━━━━━━━━━━━━━━━"));
	    player.sendMessage(TextUtil.color("&6&l売却が完了しました！"));
	    player.sendMessage(TextUtil.color("&7店舗: &e" + shoptype.getName()));
	    player.sendMessage(TextUtil.color("&7金の重量: &e" + String.format("%.2f", weight) + "g"));
	    player.sendMessage(TextUtil.color("&7受取額: &a+$" + String.format("%.2f", price)));
	    player.sendMessage(TextUtil.color("&6&l━━━━━━━━━━━━━━━━━━━━"));
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player)) return;
		Player player = (Player)event.getPlayer();
	    if (event.getInventory().getType() != InventoryType.HOPPER) return;
	    if (event.getView().getTitle() == null) return;
	    if (!event.getView().getTitle().equals("金を売却する")) return;
	    Inventory inv = event.getInventory();
	    for (int slot = 0; slot < 4; slot++)
	    	if (inv.getItem(slot) != null)
	    		player.getWorld().dropItem(player.getLocation(), inv.getItem(slot));
	}

}
